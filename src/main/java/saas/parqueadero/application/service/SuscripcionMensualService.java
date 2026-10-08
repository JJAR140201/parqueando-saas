package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.CreateSuscripcionMensualRequest;
import saas.parqueadero.application.dto.SuscripcionMensualResponse;
import saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.port.in.SuscripcionMensualUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.MensualidadEventosPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuscripcionMensualService implements SuscripcionMensualUseCase {

    private final SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final ClienteService clienteService;
    private final MensualidadEventosPort eventosPort;

    @Value("${app.mensualidad.alerta.dias-anticipacion}")
    private int diasAnticipacion;

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

        Cliente cliente = clienteService.resolverOCrear(scope.empresaId(), request.getTelefono().trim(),
            request.getNombreCliente(), request.getWhatsappHabilitado(), "Cliente " + placaNormalizada);

        SuscripcionMensual created = suscripcionMensualRepositoryPort.save(SuscripcionMensual.builder()
            .placa(placaNormalizada)
            .tipoVehiculo(request.getTipoVehiculo())
            .valorMensual(request.getValorMensual())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .activa(true)
            .telefono(request.getTelefono().trim())
            .clienteId(cliente.getId())
            .alertaVencimientoEnviada(false)
            .sedeId(scope.sedeId())
            .empresaId(scope.empresaId())
            .build());

        log.debug("[SuscripcionMensualService] Suscripcion creada id={} placa={} empresaId={} sedeId={}",
            created.getId(), created.getPlaca(), created.getEmpresaId(), created.getSedeId());
        // La notificacion se entrega despues de confirmar la transaccion y no puede hacerla fallar
        eventosPort.publicar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, created.getId());
        return enriquecer(List.of(toResponse(created))).get(0);
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

        boolean fechaFinCambio = !request.getFechaFin().equals(existing.getFechaFin());
        Cliente cliente = clienteService.resolverOCrear(existing.getEmpresaId(), request.getTelefono().trim(),
            request.getNombreCliente(), request.getWhatsappHabilitado(), "Cliente " + existing.getPlaca());

        SuscripcionMensual updated = suscripcionMensualRepositoryPort.save(SuscripcionMensual.builder()
            .id(existing.getId())
            .placa(existing.getPlaca())
            .tipoVehiculo(request.getTipoVehiculo())
            .valorMensual(request.getValorMensual())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .activa(request.getActiva() != null ? request.getActiva() : existing.getActiva())
            .telefono(request.getTelefono().trim())
            .clienteId(cliente.getId())
            .alertaVencimientoEnviada(fechaFinCambio ? false : existing.getAlertaVencimientoEnviada())
            .sedeId(existing.getSedeId())
            .empresaId(existing.getEmpresaId())
            .build());

        log.debug("[SuscripcionMensualService] Suscripcion actualizada id={} placa={}", updated.getId(), updated.getPlaca());
        // Renovar = pagar: se extiende la fecha de fin o se reactiva una mensualidad inactiva
        boolean renovada = Boolean.TRUE.equals(updated.getActiva())
            && (request.getFechaFin().isAfter(existing.getFechaFin()) || Boolean.FALSE.equals(existing.getActiva()));
        if (renovada) {
            eventosPort.publicar(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, updated.getId());
        }
        return enriquecer(List.of(toResponse(updated))).get(0);
    }

    @Override
    public List<SuscripcionMensualResponse> listSuscripciones(Long empresaId, Long sedeId, String placa) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        
        // Si es SUPER_ADMIN sin filtros, devuelve todas las suscripciones de todas las empresas
        if (hasRole(currentUser, RolUsuario.SUPER_ADMIN) && empresaId == null && sedeId == null) {
            String placaFiltro = placa == null ? null : placa.trim().toUpperCase();
            return enriquecer(suscripcionMensualRepositoryPort.findAll()
                .stream()
                .filter(s -> placaFiltro == null || s.getPlaca().contains(placaFiltro))
                .map(this::toResponse)
                .collect(Collectors.toList()));
        }
        
        // Para ADMIN/OPERARIO o SUPER_ADMIN con filtros, usa el scope normal
        Scope scope = resolveScope(currentUser, empresaId, sedeId);
        String placaFiltro = placa == null ? null : placa.trim().toUpperCase();
        return enriquecer(suscripcionMensualRepositoryPort.findByEmpresaIdAndSedeId(scope.empresaId(), scope.sedeId())
            .stream()
            .filter(s -> placaFiltro == null || s.getPlaca().contains(placaFiltro))
            .map(this::toResponse)
            .collect(Collectors.toList()));
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
            .telefono(existing.getTelefono())
            .alertaVencimientoEnviada(existing.getAlertaVencimientoEnviada())
            .sedeId(existing.getSedeId())
            .empresaId(existing.getEmpresaId())
            .build());

        log.warn("[SuscripcionMensualService] Suscripcion cancelada id={} placa={}", existing.getId(), existing.getPlaca());
    }

    @Override
    public List<SuscripcionMensualResponse> listProximasAVencer(Long empresaId, Long sedeId) {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(diasAnticipacion);

        List<SuscripcionMensual> candidatas;
        if (hasRole(currentUser, RolUsuario.SUPER_ADMIN) && empresaId == null && sedeId == null) {
            candidatas = suscripcionMensualRepositoryPort.findActivasConFinEntre(hoy, limite);
        } else {
            Scope scope = resolveScope(currentUser, empresaId, sedeId);
            candidatas = suscripcionMensualRepositoryPort.findByEmpresaIdAndSedeId(scope.empresaId(), scope.sedeId());
        }

        return enriquecer(candidatas.stream()
            .filter(s -> Boolean.TRUE.equals(s.getActiva()))
            .filter(s -> s.getFechaFin() != null && !s.getFechaFin().isBefore(hoy) && !s.getFechaFin().isAfter(limite))
            .sorted(Comparator.comparing(SuscripcionMensual::getFechaFin))
            .map(this::toResponse)
            .collect(Collectors.toList()));
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

    /** Completa nombre y consentimiento del cliente con una sola consulta para toda la lista. */
    private List<SuscripcionMensualResponse> enriquecer(List<SuscripcionMensualResponse> respuestas) {
        Set<Long> ids = respuestas.stream().map(SuscripcionMensualResponse::getClienteId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Cliente> clientes = clienteService.porIds(ids);
        respuestas.forEach(r -> {
            Cliente cliente = r.getClienteId() == null ? null : clientes.get(r.getClienteId());
            if (cliente != null) {
                r.setNombreCliente(cliente.getNombre());
                r.setWhatsappHabilitado(cliente.isWhatsappHabilitado());
            }
        });
        return respuestas;
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
            .telefono(suscripcion.getTelefono())
            .clienteId(suscripcion.getClienteId())
            .sedeId(suscripcion.getSedeId())
            .empresaId(suscripcion.getEmpresaId())
            .build();
    }

    private record Scope(Long empresaId, Long sedeId) {
    }
}
