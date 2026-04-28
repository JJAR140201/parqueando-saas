package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import saas.parqueadero.application.dto.ReporteRegistroResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.port.in.ReporteParqueoUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

@Service
@RequiredArgsConstructor
public class ReporteParqueoService implements ReporteParqueoUseCase {

    private final RegistroParqueoRepositoryPort registroParqueoRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;

    @Override
    public List<ReporteRegistroResponse> getReporte(Long empresaId, Long sedeId, EstadoRegistroParqueo estado, LocalDate desde, LocalDate hasta) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        RolUsuario rol = resolveRol(currentUser);

        Scope scope = resolveScopeByRole(currentUser, rol, empresaId, sedeId);

        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        List<RegistroParqueo> registros = registroParqueoRepositoryPort
            .findReporte(scope.empresaId(), scope.sedeId(), estado, desdeDateTime, hastaDateTime);

        Map<String, String> sedesByScopeAndId = new HashMap<>();
        Map<Long, String> usuariosById = new HashMap<>();

        return registros
            .stream()
            .map(registro -> toResponse(registro, sedesByScopeAndId, usuariosById))
            .collect(Collectors.toList());
    }

    private RolUsuario resolveRol(AuthenticatedUser user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }

        boolean isSuperAdmin = hasRole(user, RolUsuario.SUPER_ADMIN);
        if (isSuperAdmin) {
            return RolUsuario.SUPER_ADMIN;
        }

        boolean isAdmin = hasRole(user, RolUsuario.ADMIN);
        if (isAdmin) {
            return RolUsuario.ADMIN;
        }

        boolean isOperario = hasRole(user, RolUsuario.OPERARIO);
        if (isOperario) {
            return RolUsuario.OPERARIO;
        }

        throw new BusinessException("Rol no autorizado para consultar reportes");
    }

    private Scope resolveScopeByRole(AuthenticatedUser user, RolUsuario rol, Long empresaIdParam, Long sedeIdParam) {
        if (rol == RolUsuario.SUPER_ADMIN) {
            return new Scope(empresaIdParam, sedeIdParam);
        }

        if (user.getEmpresaId() == null || user.getSedeId() == null) {
            throw new BusinessException("El token no contiene empresaId y sedeId requeridos para este rol");
        }

        if (empresaIdParam != null && !empresaIdParam.equals(user.getEmpresaId())) {
            throw new BusinessException("No puedes consultar reportes de otra empresa");
        }

        if (sedeIdParam != null && !sedeIdParam.equals(user.getSedeId())) {
            throw new BusinessException("No puedes consultar reportes de otra sede");
        }

        return new Scope(user.getEmpresaId(), user.getSedeId());
    }

    private boolean hasRole(AuthenticatedUser user, RolUsuario rol) {
        return user.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(rol.name()));
    }

    private ReporteRegistroResponse toResponse(
        RegistroParqueo r,
        Map<String, String> sedesByScopeAndId,
        Map<Long, String> usuariosById
    ) {
        Long minutos = null;
        if (r.getFechaEntrada() != null && r.getFechaSalida() != null) {
            minutos = ChronoUnit.MINUTES.between(r.getFechaEntrada(), r.getFechaSalida());
        }

        String sedeNombre = resolveSedeNombre(r, sedesByScopeAndId);
        String usuarioNombre = resolveUsuarioNombre(r, usuariosById);

        return ReporteRegistroResponse.builder()
            .id(r.getId())
            .placa(r.getPlaca())
            .tipoVehiculo(r.getTipoVehiculo())
            .fechaEntrada(r.getFechaEntrada())
            .fechaSalida(r.getFechaSalida())
            .minutosEstadia(minutos)
            .totalPagado(r.getTotalPagado())
            .estado(r.getEstado())
            .sedeId(r.getSedeId())
            .sedeNombre(sedeNombre)
            .usuarioId(r.getUsuarioId())
            .usuarioNombre(usuarioNombre)
            .build();
    }

    private String resolveSedeNombre(RegistroParqueo registro, Map<String, String> sedesByScopeAndId) {
        if (registro.getSedeId() == null || registro.getEmpresaId() == null) {
            return null;
        }

        String key = registro.getEmpresaId() + ":" + registro.getSedeId();
        if (sedesByScopeAndId.containsKey(key)) {
            return sedesByScopeAndId.get(key);
        }

        String nombre = sedeRepositoryPort.findByIdAndEmpresaId(registro.getSedeId(), registro.getEmpresaId())
            .map(sede -> sede.getNombre())
            .orElse(null);
        sedesByScopeAndId.put(key, nombre);
        return nombre;
    }

    private String resolveUsuarioNombre(RegistroParqueo registro, Map<Long, String> usuariosById) {
        if (registro.getUsuarioId() == null) {
            return null;
        }

        if (usuariosById.containsKey(registro.getUsuarioId())) {
            return usuariosById.get(registro.getUsuarioId());
        }

        String nombre = usuarioRepositoryPort.findById(registro.getUsuarioId())
            .map(usuario -> usuario.getUsername())
            .orElse(null);
        usuariosById.put(registro.getUsuarioId(), nombre);
        return nombre;
    }

    private record Scope(Long empresaId, Long sedeId) {
    }
}
