package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.CreateEmpresaResponse;
import saas.parqueadero.application.dto.CreateEmpresaUserRequest;
import saas.parqueadero.application.dto.IssueLicenciaRequest;
import saas.parqueadero.application.dto.LicenciaAdminRequest;
import saas.parqueadero.application.dto.LicenciaIssuedResponse;
import saas.parqueadero.application.dto.LicenciaRedemptionRequest;
import saas.parqueadero.application.dto.LicenciaSummaryResponse;
import saas.parqueadero.application.dto.LicenciaValidationResponse;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.application.dto.ValidateLicenciaRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.EstadoLicencia;
import saas.parqueadero.domain.model.Licencia;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.LicenciaUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.LicenciaRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;
import saas.parqueadero.licensing.LicenseSerialCodec;
import saas.parqueadero.licensing.LicenseSerialInvalidException;
import saas.parqueadero.licensing.LicenseSerialPayload;

@Service
@RequiredArgsConstructor
@Slf4j
public class LicenciaService implements LicenciaUseCase {

    private static final int DURACION_DIAS_DEFAULT = 365;

    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final LicenciaRepositoryPort licenciaRepositoryPort;
    private final EmpresaRepositoryPort empresaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TenantProvisioningService tenantProvisioningService;
    private final TokenIssuanceService tokenIssuanceService;
    private final LicenseSerialCodec licenseSerialCodec;

    @Override
    @Transactional
    public LicenciaIssuedResponse issue(IssueLicenciaRequest request) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        int dias = request.getDuracionDias() != null ? request.getDuracionDias() : DURACION_DIAS_DEFAULT;
        LocalDate fechaExpiracion = LocalDate.now().plusDays(dias);
        String codigoLegible = licenseSerialCodec.generate(fechaExpiracion);

        Licencia creada = licenciaRepositoryPort.save(Licencia.builder()
            .codigo(normalizar(codigoLegible))
            .estado(EstadoLicencia.PENDIENTE)
            .fechaExpiracion(fechaExpiracion)
            .fechaEmision(LocalDateTime.now())
            .nota(request.getNota())
            .emitidaPorUsuarioId(currentUser.getUsuarioId())
            .build());

        log.info("[LicenciaService] Licencia emitida id={} expira={} por usuarioId={}",
            creada.getId(), creada.getFechaExpiracion(), currentUser.getUsuarioId());

        return LicenciaIssuedResponse.builder()
            .id(creada.getId())
            .codigo(codigoLegible)
            .estado(creada.getEstado().name())
            .fechaEmision(creada.getFechaEmision())
            .fechaExpiracion(creada.getFechaExpiracion())
            .nota(creada.getNota())
            .build();
    }

    @Override
    public List<LicenciaSummaryResponse> list() {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Map<Long, String> nombresPorEmpresa = empresaRepositoryPort.findAll().stream()
            .collect(Collectors.toMap(Empresa::getId, Empresa::getNombre));

        return licenciaRepositoryPort.findAll().stream()
            .map(licencia -> toSummary(licencia, nombresPorEmpresa))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LicenciaSummaryResponse revoke(Long licenciaId) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Licencia licencia = licenciaRepositoryPort.findById(licenciaId)
            .orElseThrow(() -> new ResourceNotFoundException("Licencia no encontrada"));

        licencia.setEstado(EstadoLicencia.REVOCADA);
        licencia.setFechaRevocacion(LocalDateTime.now());
        Licencia revocada = licenciaRepositoryPort.save(licencia);

        log.warn("[LicenciaService] Licencia revocada id={} por usuarioId={}", licenciaId, currentUser.getUsuarioId());

        Map<Long, String> nombresPorEmpresa = empresaRepositoryPort.findAll().stream()
            .collect(Collectors.toMap(Empresa::getId, Empresa::getNombre));
        return toSummary(revocada, nombresPorEmpresa);
    }

    @Override
    public LicenciaValidationResponse validate(ValidateLicenciaRequest request) {
        String normalizado = normalizar(request.getCodigo());

        LicenseSerialPayload payload;
        try {
            payload = licenseSerialCodec.verify(normalizado);
        } catch (LicenseSerialInvalidException ex) {
            return LicenciaValidationResponse.builder()
                .valida(false)
                .mensaje("El codigo ingresado no es valido")
                .build();
        }

        return licenciaRepositoryPort.findByCodigo(normalizado)
            .map(licencia -> evaluarEstado(licencia, payload))
            .orElseGet(() -> LicenciaValidationResponse.builder()
                .valida(false)
                .mensaje("El codigo ingresado no esta registrado")
                .build());
    }

    @Override
    @Transactional
    public LoginResponse redeem(LicenciaRedemptionRequest request) {
        String normalizado = normalizar(request.getCodigo());

        try {
            licenseSerialCodec.verify(normalizado);
        } catch (LicenseSerialInvalidException ex) {
            throw new BusinessException("El codigo de licencia no es valido");
        }

        Licencia licencia = licenciaRepositoryPort.findByCodigo(normalizado)
            .orElseThrow(() -> new BusinessException("El codigo de licencia no existe"));

        if (licencia.getEstado() == EstadoLicencia.REVOCADA) {
            throw new BusinessException("Este codigo de licencia fue revocado");
        }
        if (licencia.getEstado() == EstadoLicencia.REDIMIDA) {
            throw new BusinessException("Este codigo de licencia ya fue utilizado");
        }
        if (licencia.getFechaExpiracion().isBefore(LocalDate.now())) {
            throw new BusinessException("Este codigo de licencia esta vencido");
        }

        LicenciaAdminRequest adminRequest = request.getAdmin();

        CreateEmpresaResponse empresaCreada = tenantProvisioningService.createEmpresaWithSedes(request.getEmpresa());

        RegisterUserResponse adminCreado = tenantProvisioningService.createUserForEmpresa(CreateEmpresaUserRequest.builder()
            .nombre(adminRequest.getNombre())
            .username(adminRequest.getUsername())
            .password(adminRequest.getPassword())
            .rol(RolUsuario.ADMIN)
            .empresaId(empresaCreada.getEmpresaId())
            .sedeId(empresaCreada.getSedes().get(0).getId())
            .build());

        licencia.setEstado(EstadoLicencia.REDIMIDA);
        licencia.setEmpresaId(empresaCreada.getEmpresaId());
        licencia.setFechaRedencion(LocalDateTime.now());

        try {
            licenciaRepositoryPort.save(licencia);
        } catch (OptimisticLockingFailureException ex) {
            throw new BusinessException("Este codigo de licencia ya fue utilizado");
        }

        log.info("[LicenciaService] Licencia redimida id={} empresaId={} adminUsuarioId={}",
            licencia.getId(), empresaCreada.getEmpresaId(), adminCreado.getUsuarioId());

        Usuario usuarioCompleto = usuarioRepositoryPort.findById(adminCreado.getUsuarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario administrador no encontrado"));

        return tokenIssuanceService.buildLoginResponse(usuarioCompleto);
    }

    private LicenciaValidationResponse evaluarEstado(Licencia licencia, LicenseSerialPayload payload) {
        if (licencia.getEstado() == EstadoLicencia.REVOCADA) {
            return LicenciaValidationResponse.builder()
                .valida(false)
                .mensaje("Este codigo de licencia fue revocado")
                .build();
        }
        if (licencia.getEstado() == EstadoLicencia.REDIMIDA) {
            return LicenciaValidationResponse.builder()
                .valida(false)
                .mensaje("Este codigo de licencia ya fue utilizado")
                .build();
        }
        if (licencia.getFechaExpiracion().isBefore(LocalDate.now())) {
            return LicenciaValidationResponse.builder()
                .valida(false)
                .mensaje("Este codigo de licencia esta vencido")
                .build();
        }

        return LicenciaValidationResponse.builder()
            .valida(true)
            .mensaje("Codigo valido")
            .fechaExpiracion(payload.expiresAt())
            .build();
    }

    private LicenciaSummaryResponse toSummary(Licencia licencia, Map<Long, String> nombresPorEmpresa) {
        return LicenciaSummaryResponse.builder()
            .id(licencia.getId())
            .codigoEnmascarado(enmascarar(licencia.getCodigo()))
            .estado(licencia.getEstado().name())
            .fechaEmision(licencia.getFechaEmision())
            .fechaExpiracion(licencia.getFechaExpiracion())
            .fechaRedencion(licencia.getFechaRedencion())
            .fechaRevocacion(licencia.getFechaRevocacion())
            .empresaId(licencia.getEmpresaId())
            .empresaNombre(licencia.getEmpresaId() == null ? null : nombresPorEmpresa.get(licencia.getEmpresaId()))
            .nota(licencia.getNota())
            .build();
    }

    private String enmascarar(String codigoNormalizado) {
        if (codigoNormalizado.length() <= 4) {
            return codigoNormalizado;
        }
        return "****" + codigoNormalizado.substring(codigoNormalizado.length() - 4);
    }

    private String normalizar(String codigo) {
        return codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private void enforceSuperAdmin(AuthenticatedUser currentUser) {
        if (currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }

        boolean isSuperAdmin = currentUser.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(RolUsuario.SUPER_ADMIN.name()));

        if (!isSuperAdmin) {
            throw new BusinessException("Solo el SUPER_ADMIN puede administrar licencias");
        }
    }
}
