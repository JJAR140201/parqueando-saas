package saas.parqueadero.application.service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.CreateEmpresaRequest;
import saas.parqueadero.application.dto.CreateEmpresaResponse;
import saas.parqueadero.application.dto.CreateEmpresaUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.application.dto.SedeSummaryResponse;
import saas.parqueadero.application.dto.UpsertTarifasRequest;
import saas.parqueadero.application.dto.UpsertTarifasResponse;
import saas.parqueadero.application.dto.UserSummaryResponse;
import saas.parqueadero.application.dto.UpdateEmpresaRequest;
import saas.parqueadero.application.dto.UpdateEmpresaSedeRequest;
import saas.parqueadero.application.dto.UpdateUserRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.model.TipoVehiculo;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.SuperAdminUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.TarifaRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminService implements SuperAdminUseCase {

    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final EmpresaRepositoryPort empresaRepositoryPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TarifaRepositoryPort tarifaRepositoryPort;
    private final RegistroParqueoRepositoryPort registroParqueoRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<SedeSummaryResponse> listSedesByEmpresa(Long empresaId) {
        log.info("[SuperAdminService] Listar sedes de empresaId={}", empresaId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        return sedeRepositoryPort.findByEmpresaId(empresaId).stream()
            .map(sede -> SedeSummaryResponse.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .capacidadTotal(sede.getCapacidadTotal())
                .capacidadActual(sede.getCapacidadActual())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<UserSummaryResponse> listUsersByEmpresa(Long empresaId) {
        log.info("[SuperAdminService] Listar usuarios de empresaId={}", empresaId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Empresa empresa = empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Map<Long, String> sedesById = sedeRepositoryPort.findByEmpresaId(empresaId).stream()
            .collect(Collectors.toMap(Sede::getId, Sede::getNombre));

        return usuarioRepositoryPort.findByEmpresaId(empresaId).stream()
            .map(usuario -> UserSummaryResponse.builder()
                .usuarioId(usuario.getId())
                .nombre(usuario.getNombre())
                .username(usuario.getUsername())
                .rol(usuario.getRol().name())
                .empresaId(usuario.getEmpresaId())
                .empresaNombre(empresa.getNombre())
                .sedeId(usuario.getSedeId())
                .sedeNombre(usuario.getSedeId() == null ? null : sedesById.get(usuario.getSedeId()))
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<CreateEmpresaResponse> listEmpresas() {
        log.info("[SuperAdminService] Listar empresas");
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        return empresaRepositoryPort.findAll().stream()
            .map(empresa -> {
                List<SedeSummaryResponse> sedes = sedeRepositoryPort.findByEmpresaId(empresa.getId()).stream()
                    .map(sede -> SedeSummaryResponse.builder()
                        .id(sede.getId())
                        .nombre(sede.getNombre())
                        .capacidadTotal(sede.getCapacidadTotal())
                        .capacidadActual(sede.getCapacidadActual())
                        .build())
                    .collect(Collectors.toList());
                return CreateEmpresaResponse.builder()
                    .empresaId(empresa.getId())
                    .nit(empresa.getNit())
                    .nombre(empresa.getNombre())
                    .sedes(sedes)
                    .build();
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CreateEmpresaResponse createEmpresaWithSedes(CreateEmpresaRequest request) {
        log.info("[SuperAdminService] Crear empresa nit={} nombre={} sedes={}", request.getNit(), request.getNombre(), request.getSedes().size());
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        String normalizedNit = request.getNit().trim();
        empresaRepositoryPort.findByNit(normalizedNit).ifPresent(existing -> {
            throw new BusinessException("Ya existe una empresa registrada con el mismo NIT");
        });

        Empresa createdEmpresa = empresaRepositoryPort.save(Empresa.builder()
            .nit(normalizedNit)
            .nombre(request.getNombre().trim())
            .build());

        List<SedeSummaryResponse> sedes = new ArrayList<>();
        request.getSedes().forEach(sedeRequest -> {
            Sede createdSede = sedeRepositoryPort.save(Sede.builder()
                .nombre(sedeRequest.getNombre().trim())
                .capacidadTotal(sedeRequest.getCapacidadTotal())
                .capacidadActual(sedeRequest.getCapacidadTotal())
                .empresaId(createdEmpresa.getId())
                .build());

            sedes.add(SedeSummaryResponse.builder()
                .id(createdSede.getId())
                .nombre(createdSede.getNombre())
                .capacidadTotal(createdSede.getCapacidadTotal())
                .capacidadActual(createdSede.getCapacidadActual())
                .build());
        });

        return CreateEmpresaResponse.builder()
            .empresaId(createdEmpresa.getId())
            .nit(createdEmpresa.getNit())
            .nombre(createdEmpresa.getNombre())
            .sedes(sedes)
            .build();
    }

    @Override
    @Transactional
    public RegisterUserResponse createUserForEmpresa(CreateEmpresaUserRequest request) {
        log.info("[SuperAdminService] Crear usuario por empresa username={} rol={} empresaId={} sedeId={}",
            request.getUsername(), request.getRol(), request.getEmpresaId(), request.getSedeId());
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        if (request.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("Use /api/v1/auth/register para crear SUPER_ADMIN");
        }

        empresaRepositoryPort.findById(request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sede sede = sedeRepositoryPort.findByIdAndEmpresaId(request.getSedeId(), request.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("La sede no existe para la empresa indicada"));

        String normalizedUsername = request.getUsername().trim();
        String normalizedNombre = request.getNombre() == null || request.getNombre().isBlank()
            ? normalizedUsername
            : request.getNombre().trim();
        usuarioRepositoryPort.findByUsernameAndEmpresaId(normalizedUsername, request.getEmpresaId())
            .ifPresent(user -> {
                throw new BusinessException("Ya existe un usuario con ese username en la empresa");
            });

        Usuario created = usuarioRepositoryPort.save(Usuario.builder()
            .nombre(normalizedNombre)
            .username(normalizedUsername)
            .password(passwordEncoder.encode(request.getPassword()))
            .rol(request.getRol())
            .empresaId(request.getEmpresaId())
            .sedeId(sede.getId())
            .build());

        return RegisterUserResponse.builder()
            .usuarioId(created.getId())
            .nombre(created.getNombre())
            .username(created.getUsername())
            .rol(created.getRol().name())
            .empresaId(created.getEmpresaId())
            .sedeId(created.getSedeId())
            .build();
    }

    private void enforceSuperAdmin(AuthenticatedUser currentUser) {
        if (currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }

        boolean isSuperAdmin = currentUser.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(RolUsuario.SUPER_ADMIN.name()));

        if (!isSuperAdmin) {
            throw new BusinessException("Solo el SUPER_ADMIN puede crear empresas y sedes");
        }
    }

    @Override
    @Transactional
    public CreateEmpresaResponse updateEmpresaWithSedes(Long empresaId, UpdateEmpresaRequest request) {
        log.info("[SuperAdminService] Actualizar empresa empresaId={} nit={} sedes={}", empresaId, request.getNit(), request.getSedes().size());
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Empresa existing = empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        String normalizedNit = request.getNit().trim();
        empresaRepositoryPort.findByNit(normalizedNit)
            .filter(empresa -> !empresa.getId().equals(empresaId))
            .ifPresent(empresa -> {
                throw new BusinessException("Ya existe otra empresa con ese NIT");
            });

        Empresa updatedEmpresa = empresaRepositoryPort.save(Empresa.builder()
            .id(existing.getId())
            .nit(normalizedNit)
            .nombre(request.getNombre().trim())
            .build());

        List<Sede> currentSedes = sedeRepositoryPort.findByEmpresaId(empresaId);
        Map<Long, Sede> currentById = currentSedes.stream()
            .collect(Collectors.toMap(Sede::getId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        List<SedeSummaryResponse> updatedSedes = new ArrayList<>();

        for (UpdateEmpresaSedeRequest sedeRequest : request.getSedes()) {
            Sede saved;
            if (sedeRequest.getId() == null) {
                saved = sedeRepositoryPort.save(Sede.builder()
                    .nombre(sedeRequest.getNombre().trim())
                    .capacidadTotal(sedeRequest.getCapacidadTotal())
                    .capacidadActual(resolveCapacidadActual(null, sedeRequest.getCapacidadActual(), sedeRequest.getCapacidadTotal()))
                    .empresaId(empresaId)
                    .build());
            } else {
                Sede existingSede = currentById.get(sedeRequest.getId());
                if (existingSede == null) {
                    throw new ResourceNotFoundException("La sede indicada no pertenece a la empresa");
                }

                saved = sedeRepositoryPort.save(Sede.builder()
                    .id(existingSede.getId())
                    .nombre(sedeRequest.getNombre().trim())
                    .capacidadTotal(sedeRequest.getCapacidadTotal())
                    .capacidadActual(resolveCapacidadActual(existingSede.getCapacidadActual(), sedeRequest.getCapacidadActual(), sedeRequest.getCapacidadTotal()))
                    .empresaId(empresaId)
                    .build());
                keepIds.add(saved.getId());
            }

            keepIds.add(saved.getId());
            updatedSedes.add(SedeSummaryResponse.builder()
                .id(saved.getId())
                .nombre(saved.getNombre())
                .capacidadTotal(saved.getCapacidadTotal())
                .capacidadActual(saved.getCapacidadActual())
                .build());
        }

        for (Sede currentSede : currentSedes) {
            if (!keepIds.contains(currentSede.getId())) {
                if (usuarioRepositoryPort.existsBySedeIdAndEmpresaId(currentSede.getId(), empresaId)) {
                    throw new BusinessException("No puede eliminar una sede que tiene usuarios asignados");
                }
                sedeRepositoryPort.deleteByIdAndEmpresaId(currentSede.getId(), empresaId);
            }
        }

        return CreateEmpresaResponse.builder()
            .empresaId(updatedEmpresa.getId())
            .nit(updatedEmpresa.getNit())
            .nombre(updatedEmpresa.getNombre())
            .sedes(updatedSedes)
            .build();
    }

    @Override
    @Transactional
    public UpsertTarifasResponse upsertTarifas(Long empresaId, Long sedeId, UpsertTarifasRequest request) {
        log.info("[SuperAdminService] Upsert tarifas empresaId={} sedeId={}", empresaId, sedeId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        sedeRepositoryPort.findByIdAndEmpresaId(sedeId, empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("La sede no existe para la empresa indicada"));

        Tarifa tarifaCarro = upsertTarifaPorTipo(
            empresaId,
            sedeId,
            TipoVehiculo.CARRO,
            request.getValorFraccionCarro(),
            request.getMinutosFraccionCarro()
        );

        Tarifa tarifaMoto = upsertTarifaPorTipo(
            empresaId,
            sedeId,
            TipoVehiculo.MOTO,
            request.getValorFraccionMoto(),
            request.getMinutosFraccionMoto()
        );

        return UpsertTarifasResponse.builder()
            .empresaId(empresaId)
            .sedeId(sedeId)
            .valorFraccionCarro(tarifaCarro.getValorFraccion())
            .minutosFraccionCarro(tarifaCarro.getMinutosFraccion())
            .valorFraccionMoto(tarifaMoto.getValorFraccion())
            .minutosFraccionMoto(tarifaMoto.getMinutosFraccion())
            .build();
    }

    @Override
    @Transactional
    public void deleteEmpresa(Long empresaId) {
        log.warn("[SuperAdminService] Eliminar empresa empresaId={}", empresaId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        registroParqueoRepositoryPort.deleteByEmpresaId(empresaId);
        tarifaRepositoryPort.deleteByEmpresaId(empresaId);
        usuarioRepositoryPort.deleteByEmpresaId(empresaId);
        sedeRepositoryPort.deleteByEmpresaId(empresaId);
        empresaRepositoryPort.deleteById(empresaId);
    }

    @Override
    @Transactional
    public RegisterUserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("[SuperAdminService] Actualizar usuario userId={} username={} rol={} sedeId={}",
            userId, request.getUsername(), request.getRol(), request.getSedeId());
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Usuario existingUser = usuarioRepositoryPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Sede sede = sedeRepositoryPort.findByIdAndEmpresaId(request.getSedeId(), existingUser.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("La sede no existe para la empresa del usuario"));

        String username = request.getUsername() != null && !request.getUsername().isBlank()
            ? request.getUsername().trim()
            : existingUser.getUsername();
        String nombre = request.getNombre() != null && !request.getNombre().isBlank()
            ? request.getNombre().trim()
            : (existingUser.getNombre() == null || existingUser.getNombre().isBlank() ? username : existingUser.getNombre());

        usuarioRepositoryPort.findByUsernameAndEmpresaId(username, existingUser.getEmpresaId())
            .filter(user -> !user.getId().equals(existingUser.getId()))
            .ifPresent(user -> {
                throw new BusinessException("Ya existe otro usuario con ese username en la empresa");
            });

        String encodedPassword = request.getPassword() != null && !request.getPassword().isBlank()
            ? passwordEncoder.encode(request.getPassword())
            : existingUser.getPassword();

        Usuario saved = usuarioRepositoryPort.save(Usuario.builder()
            .id(existingUser.getId())
            .nombre(nombre)
            .username(username)
            .password(encodedPassword)
            .rol(request.getRol())
            .sedeId(sede.getId())
            .empresaId(existingUser.getEmpresaId())
            .build());

        return RegisterUserResponse.builder()
            .usuarioId(saved.getId())
            .nombre(saved.getNombre())
            .username(saved.getUsername())
            .rol(saved.getRol().name())
            .empresaId(saved.getEmpresaId())
            .sedeId(saved.getSedeId())
            .build();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.warn("[SuperAdminService] Eliminar usuario userId={}", userId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        enforceSuperAdmin(currentUser);

        Usuario existingUser = usuarioRepositoryPort.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (existingUser.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("No se permite eliminar un usuario SUPER_ADMIN");
        }

        usuarioRepositoryPort.deleteById(userId);
    }

    private Integer resolveCapacidadActual(Integer existingCapacidadActual, Integer requestedCapacidadActual, Integer capacidadTotal) {
        int capacidad = requestedCapacidadActual != null
            ? requestedCapacidadActual
            : (existingCapacidadActual != null ? existingCapacidadActual : capacidadTotal);

        if (capacidad < 0) {
            throw new BusinessException("La capacidad actual no puede ser negativa");
        }

        return Math.min(capacidad, capacidadTotal);
    }

    private Tarifa upsertTarifaPorTipo(
        Long empresaId,
        Long sedeId,
        TipoVehiculo tipoVehiculo,
        BigDecimal valorFraccion,
        Integer minutosFraccion
    ) {
        Tarifa existing = tarifaRepositoryPort
            .findBySedeIdAndEmpresaIdAndTipoVehiculo(sedeId, empresaId, tipoVehiculo)
            .orElse(null);

        Tarifa tarifa = Tarifa.builder()
            .id(existing != null ? existing.getId() : null)
            .tipoVehiculo(tipoVehiculo)
            .valorFraccion(valorFraccion)
            .minutosFraccion(minutosFraccion)
            .sedeId(sedeId)
            .empresaId(empresaId)
            .build();

        return tarifaRepositoryPort.save(tarifa);
    }
}
