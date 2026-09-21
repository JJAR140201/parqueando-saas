package saas.parqueadero.application.service;

import java.util.ArrayList;
import java.util.List;
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
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

/**
 * Logica pura de aprovisionamiento de empresa+sedes y de usuarios de empresa, sin el
 * chequeo de rol SUPER_ADMIN (que requiere un contexto autenticado). Extraida de
 * {@link SuperAdminService} para poder reutilizarla desde el flujo publico de activacion
 * de licencias, donde no hay un usuario autenticado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final EmpresaRepositoryPort empresaRepositoryPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateEmpresaResponse createEmpresaWithSedes(CreateEmpresaRequest request) {
        log.info("[TenantProvisioningService] Crear empresa nit={} nombre={} sedes={}",
            request.getNit(), request.getNombre(), request.getSedes().size());

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

    @Transactional
    public RegisterUserResponse createUserForEmpresa(CreateEmpresaUserRequest request) {
        log.info("[TenantProvisioningService] Crear usuario por empresa username={} rol={} empresaId={} sedeId={}",
            request.getUsername(), request.getRol(), request.getEmpresaId(), request.getSedeId());

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
}
