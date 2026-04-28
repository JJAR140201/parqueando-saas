package saas.parqueadero.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.PrecioSalidaResponse;
import saas.parqueadero.application.dto.RegistrarEntradaRequest;
import saas.parqueadero.application.dto.RegistrarSalidaRequest;
import saas.parqueadero.application.dto.RegistroParqueoResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.CapacityUnavailableException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.port.in.RegistroParqueoUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;
import saas.parqueadero.domain.port.out.TarifaRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistroParqueoService implements RegistroParqueoUseCase {

    private final SedeRepositoryPort sedeRepositoryPort;
    private final TarifaRepositoryPort tarifaRepositoryPort;
    private final RegistroParqueoRepositoryPort registroParqueoRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;

    @Override
    @Transactional
    public RegistroParqueoResponse registrarEntrada(RegistrarEntradaRequest request) {
        log.info("[RegistroParqueoService] Inicia entrada placa={} tipoVehiculo={}", request.getPlaca(), request.getTipoVehiculo());
        AuthenticatedUser user = authenticatedUserProviderPort.getCurrentUser();
        enforceOperarioRole(user);
        Sede sede = sedeRepositoryPort.findByIdAndEmpresaId(user.getSedeId(), user.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("No existe la sede para la empresa indicada"));

        if (sede.getCapacidadActual() == null || sede.getCapacidadActual() <= 0) {
            throw new CapacityUnavailableException("No hay cupos disponibles en la sede");
        }

        registroParqueoRepositoryPort
            .findActivoByPlaca(request.getPlaca())
            .ifPresent(existing -> {
                throw new BusinessException("Ya existe un registro activo para esta placa. Debe registrar la salida antes de volver a ingresar");
            });

        RegistroParqueo registro = RegistroParqueo.builder()
            .placa(request.getPlaca().trim().toUpperCase())
            .tipoVehiculo(request.getTipoVehiculo())
            .fechaEntrada(LocalDateTime.now())
            .estado(EstadoRegistroParqueo.ACTIVO)
            .sedeId(user.getSedeId())
            .usuarioId(user.getUsuarioId())
            .empresaId(user.getEmpresaId())
            .build();

        sede.setCapacidadActual(sede.getCapacidadActual() - 1);
        sedeRepositoryPort.save(sede);

        RegistroParqueo saved = registroParqueoRepositoryPort.save(registro);
        log.info("[RegistroParqueoService] Entrada registrada id={} placa={} sedeId={} empresaId={}",
            saved.getId(), saved.getPlaca(), saved.getSedeId(), saved.getEmpresaId());
        return toResponse(saved);
    }

    @Override
    public PrecioSalidaResponse consultarPrecioSalida(String placa) {
        log.info("[RegistroParqueoService] Consultar precio salida placa={}", placa);
        AuthenticatedUser user = authenticatedUserProviderPort.getCurrentUser();
        enforceOperarioRole(user);

        RegistroParqueo registro = findRegistroActivo(placa, user);
        LocalDateTime fechaSalida = LocalDateTime.now();
        long minutosEstadia = Math.max(1, Duration.between(registro.getFechaEntrada(), fechaSalida).toMinutes());
        boolean mensualidadActiva = hasMensualidadVigente(registro, user, fechaSalida);
        BigDecimal total;
        if (mensualidadActiva) {
            total = BigDecimal.ZERO;
        } else {
            Tarifa tarifa = findTarifa(user, registro);
            total = calcularTotal(registro.getFechaEntrada(), fechaSalida, tarifa);
        }
        BigDecimal horas = BigDecimal.valueOf(minutosEstadia)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return PrecioSalidaResponse.builder()
            .placa(registro.getPlaca())
            .tipoVehiculo(registro.getTipoVehiculo())
            .tipo(registro.getTipoVehiculo() != null ? registro.getTipoVehiculo().name() : null)
            .fechaEntrada(registro.getFechaEntrada())
            .fechaSalida(fechaSalida)
            .minutosEstadia(minutosEstadia)
            .horas(horas)
            .totalPagado(total)
            .total(total)
            .mensualidadActiva(mensualidadActiva)
            .sedeId(registro.getSedeId())
            .empresaId(registro.getEmpresaId())
            .build();
    }

    @Override
    @Transactional
    public RegistroParqueoResponse registrarSalida(RegistrarSalidaRequest request) {
        log.info("[RegistroParqueoService] Inicia salida placa={}", request.getPlaca());
        AuthenticatedUser user = authenticatedUserProviderPort.getCurrentUser();
        enforceOperarioRole(user);
        RegistroParqueo registro = findRegistroActivo(request.getPlaca(), user);

        Sede sede = sedeRepositoryPort.findByIdAndEmpresaId(user.getSedeId(), user.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("No existe la sede para la empresa indicada"));

        LocalDateTime fechaSalida = LocalDateTime.now();
        boolean mensualidadActiva = hasMensualidadVigente(registro, user, fechaSalida);
        BigDecimal total;
        if (mensualidadActiva) {
            total = BigDecimal.ZERO;
        } else {
            Tarifa tarifa = findTarifa(user, registro);
            total = calcularTotal(registro.getFechaEntrada(), fechaSalida, tarifa);
        }

        registro.setFechaSalida(fechaSalida);
        registro.setEstado(EstadoRegistroParqueo.FINALIZADO);
        registro.setTotalPagado(total);

        sede.setCapacidadActual(Math.min(sede.getCapacidadTotal(), sede.getCapacidadActual() + 1));
        sedeRepositoryPort.save(sede);

        RegistroParqueo saved = registroParqueoRepositoryPort.save(registro);
        log.info("[RegistroParqueoService] Salida registrada id={} placa={} totalPagado={}",
            saved.getId(), saved.getPlaca(), saved.getTotalPagado());
        return toResponse(saved);
    }

    private RegistroParqueo findRegistroActivo(String placa, AuthenticatedUser user) {
        return registroParqueoRepositoryPort
            .findActivoByPlacaAndSedeIdAndEmpresaId(placa, user.getSedeId(), user.getEmpresaId())
            .orElseThrow(() -> new ResourceNotFoundException("No existe un registro activo para la placa indicada"));
    }

    private Tarifa findTarifa(AuthenticatedUser user, RegistroParqueo registro) {
        return tarifaRepositoryPort
            .findBySedeIdAndEmpresaIdAndTipoVehiculo(user.getSedeId(), user.getEmpresaId(), registro.getTipoVehiculo())
            .orElseThrow(() -> new ResourceNotFoundException("No existe tarifa configurada para el tipo de vehiculo en la sede"));
    }

    private BigDecimal calcularTotal(LocalDateTime fechaEntrada, LocalDateTime fechaSalida, Tarifa tarifa) {
        long minutos = Math.max(1, Duration.between(fechaEntrada, fechaSalida).toMinutes());
        int minutosFraccion = Math.max(1, tarifa.getMinutosFraccion());
        BigDecimal fracciones = BigDecimal.valueOf(minutos)
            .divide(BigDecimal.valueOf(minutosFraccion), 0, RoundingMode.CEILING);
        return tarifa.getValorFraccion().multiply(fracciones);
    }

    private boolean hasMensualidadVigente(RegistroParqueo registro, AuthenticatedUser user, LocalDateTime fechaSalida) {
        LocalDate fecha = fechaSalida.toLocalDate();
        return suscripcionMensualRepositoryPort
            .findVigenteByPlacaAndSedeIdAndEmpresaId(registro.getPlaca(), user.getSedeId(), user.getEmpresaId(), fecha)
            .isPresent();
    }

    private RegistroParqueoResponse toResponse(RegistroParqueo registro) {
        return RegistroParqueoResponse.builder()
            .id(registro.getId())
            .placa(registro.getPlaca())
            .tipoVehiculo(registro.getTipoVehiculo())
            .fechaEntrada(registro.getFechaEntrada())
            .fechaSalida(registro.getFechaSalida())
            .estado(registro.getEstado())
            .totalPagado(registro.getTotalPagado())
            .sedeId(registro.getSedeId())
            .usuarioId(registro.getUsuarioId())
            .empresaId(registro.getEmpresaId())
            .build();
    }

    private void enforceOperarioRole(AuthenticatedUser user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }

        boolean isOperario = user.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(RolUsuario.OPERARIO.name()));

        if (!isOperario) {
            throw new BusinessException("Solo el rol OPERARIO puede registrar entrada y salida de vehiculos");
        }
    }
}
