package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.CreateSuscripcionMensualRequest;
import saas.parqueadero.application.dto.SuscripcionMensualResponse;
import saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.in.SuscripcionMensualUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionMensualService implements SuscripcionMensualUseCase {

    private final SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final SedeRepositoryPort sedeRepositoryPort;

    @Override
    @Transactional
    public SuscripcionMensualResponse createSuscripcion(CreateSuscripcionMensualRequest request) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        Scope scope = resolveScope(currentUser, request.getEmpresaId(), request.getSedeId());
        validateFechas(request.getFechaInicio(), request.getFechaFin());

        String placaNormalizada = request.getPlaca().trim().toUpperCase();
        if (suscripcionMensualRepositoryPort.existsActivaOverlap(
            placaNormalizada,
            scope.sedeId(),
            scope.empresaId(),
            request.getFechaInicio(),
            request.getFechaFin()
        )) {
            throw new BusinessException("Ya existe una suscripcion activa para esta placa en el rango de fechas indicado");
        }

        SuscripcionMensual created = suscripcionMensualRepositoryPort.save(SuscripcionMensual.builder()
            .placa(placaNormalizada)
            .tipoVehiculo(request.getTipoVehiculo())
            .valorMensual(request.getValorMensual())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .activa(true)
            .sedeId(scope.sedeId())
            .empresaId(scope.empresaId())
            .build());

        log.info("[SuscripcionMensualService] Suscripcion creada id={} placa={} empresaId={} sedeId={}",
            created.getId(), created.getPlaca(), created.getEmpresaId(), created.getSedeId());
        return toResponse(created);
    }

    @Override
    @Transactional
    public SuscripcionMensualResponse updateSuscripcion(Long id, UpdateSuscripcionMensualRequest request) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        Scope scope = resolveScope(currentUser, request.getEmpresaId(), request.getSedeId());
        validateFechas(request.getFechaInicio(), request.getFechaFin());

        SuscripcionMensual existing = suscripcionMensualRepositoryPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada"));

        if (!existing.getEmpresaId().equals(scope.empresaId()) || !existing.getSedeId().equals(scope.sedeId())) {
            throw new BusinessException("No puedes editar una suscripcion fuera de tu alcance");
        }

        if (suscripcionMensualRepositoryPort.existsActivaOverlapExcludingId(
            id,
            existing.getPlaca(),
            scope.sedeId(),
            scope.empresaId(),
            request.getFechaInicio(),
            request.getFechaFin()
        )) {
            throw new BusinessException("Ya existe otra suscripcion activa para esta placa en el rango de fechas indicado");
        }

        SuscripcionMensual updated = suscripcionMensualRepositoryPort.save(SuscripcionMensual.builder()
            .id(existing.getId())
            .placa(existing.getPlaca())
            .tipoVehiculo(request.getTipoVehiculo())
            .valorMensual(request.getValorMensual())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .activa(request.getActiva() != null ? request.getActiva() : existing.getActiva())
            .sedeId(existing.getSedeId())
            .empresaId(existing.getEmpresaId())
            .build());

        log.info("[SuscripcionMensualService] Suscripcion actualizada id={} placa={}", updated.getId(), updated.getPlaca());
        return toResponse(updated);
    }

    @Override
    public List<SuscripcionMensualResponse> listSuscripciones(Long empresaId, Long sedeId, String placa) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        
        // Si es SUPER_ADMIN sin filtros, devuelve todas las suscripciones de todas las empresas
        if (hasRole(currentUser, RolUsuario.SUPER_ADMIN) && empresaId == null && sedeId == null) {
            String placaFiltro = placa == null ? null : placa.trim().toUpperCase();
            return suscripcionMensualRepositoryPort.findAll()
                .stream()
                .filter(s -> placaFiltro == null || s.getPlaca().contains(placaFiltro))
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        
        // Para ADMIN/OPERARIO o SUPER_ADMIN con filtros, usa el scope normal
        Scope scope = resolveScope(currentUser, empresaId, sedeId);
        String placaFiltro = placa == null ? null : placa.trim().toUpperCase();
        return suscripcionMensualRepositoryPort.findByEmpresaIdAndSedeId(scope.empresaId(), scope.sedeId())
            .stream()
            .filter(s -> placaFiltro == null || s.getPlaca().contains(placaFiltro))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelSuscripcion(Long id, Long empresaId, Long sedeId) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        Scope scope = resolveScope(currentUser, empresaId, sedeId);

        SuscripcionMensual existing = suscripcionMensualRepositoryPort.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada"));

        if (!existing.getEmpresaId().equals(scope.empresaId()) || !existing.getSedeId().equals(scope.sedeId())) {
            throw new BusinessException("No puedes cancelar una suscripcion fuera de tu alcance");
        }

        if (Boolean.FALSE.equals(existing.getActiva())) {
            return;
        }

        suscripcionMensualRepositoryPort.save(SuscripcionMensual.builder()
            .id(existing.getId())
            .placa(existing.getPlaca())
            .tipoVehiculo(existing.getTipoVehiculo())
            .valorMensual(existing.getValorMensual())
            .fechaInicio(existing.getFechaInicio())
            .fechaFin(existing.getFechaFin())
            .activa(false)
            .sedeId(existing.getSedeId())
            .empresaId(existing.getEmpresaId())
            .build());

        log.warn("[SuscripcionMensualService] Suscripcion cancelada id={} placa={}", existing.getId(), existing.getPlaca());
    }

    private Scope resolveScope(AuthenticatedUser user, Long empresaIdParam, Long sedeIdParam) {
        ensureAllowedRole(user);

        if (hasRole(user, RolUsuario.SUPER_ADMIN)) {
            if (empresaIdParam == null || sedeIdParam == null) {
                throw new BusinessException("SUPER_ADMIN debe indicar empresaId y sedeId");
            }
            validateSedeBelongsToEmpresa(sedeIdParam, empresaIdParam);
            return new Scope(empresaIdParam, sedeIdParam);
        }

        if (user.getEmpresaId() == null || user.getSedeId() == null) {
            throw new BusinessException("El token no contiene empresaId y sedeId requeridos");
        }

        if (empresaIdParam != null && !empresaIdParam.equals(user.getEmpresaId())) {
            throw new BusinessException("No puedes operar suscripciones de otra empresa");
        }

        if (sedeIdParam != null && !sedeIdParam.equals(user.getSedeId())) {
            throw new BusinessException("No puedes operar suscripciones de otra sede");
        }

        validateSedeBelongsToEmpresa(user.getSedeId(), user.getEmpresaId());
        return new Scope(user.getEmpresaId(), user.getSedeId());
    }

    private void validateSedeBelongsToEmpresa(Long sedeId, Long empresaId) {
        sedeRepositoryPort.findByIdAndEmpresaId(sedeId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("La sede no existe para la empresa indicada"));
    }

    private void validateFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException("La fechaFin no puede ser anterior a la fechaInicio");
        }
    }

    private void ensureAllowedRole(AuthenticatedUser user) {
        if (!hasRole(user, RolUsuario.SUPER_ADMIN)
            && !hasRole(user, RolUsuario.ADMIN)
            && !hasRole(user, RolUsuario.OPERARIO)) {
            throw new BusinessException("Rol no autorizado para gestionar suscripciones mensuales");
        }
    }

    private boolean hasRole(AuthenticatedUser user, RolUsuario rol) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(rol.name()));
    }

    private SuscripcionMensualResponse toResponse(SuscripcionMensual suscripcion) {
        LocalDate hoy = LocalDate.now();
        boolean vigenteHoy = Boolean.TRUE.equals(suscripcion.getActiva())
            && (hoy.isEqual(suscripcion.getFechaInicio()) || hoy.isAfter(suscripcion.getFechaInicio()))
            && (hoy.isEqual(suscripcion.getFechaFin()) || hoy.isBefore(suscripcion.getFechaFin()));

        return SuscripcionMensualResponse.builder()
            .id(suscripcion.getId())
            .placa(suscripcion.getPlaca())
            .tipoVehiculo(suscripcion.getTipoVehiculo())
            .valorMensual(suscripcion.getValorMensual())
            .fechaInicio(suscripcion.getFechaInicio())
            .fechaFin(suscripcion.getFechaFin())
            .activa(suscripcion.getActiva())
            .vigenteHoy(vigenteHoy)
            .sedeId(suscripcion.getSedeId())
            .empresaId(suscripcion.getEmpresaId())
            .build();
    }

    private record Scope(Long empresaId, Long sedeId) {
    }
}
