package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensualidadVencimientoNotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final EmpresaRepositoryPort empresaRepositoryPort;
    private final TwilioService twilioService;

    @Value("${app.mensualidad.alerta.dias-anticipacion}")
    private int diasAnticipacion;

    @Scheduled(cron = "${app.mensualidad.alerta.cron}", zone = "America/Bogota")
    public void notificarVencimientosProximos() {
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(diasAnticipacion);

        List<SuscripcionMensual> pendientes = suscripcionMensualRepositoryPort
            .findPendientesDeAlertaVencimiento(hoy, limite);

        log.info("[MensualidadVencimientoNotificationService] {} mensualidades por notificar (vencen entre {} y {})",
            pendientes.size(), hoy, limite);

        pendientes.forEach(this::enviarAlerta);
    }

    private void enviarAlerta(SuscripcionMensual suscripcion) {
        if (suscripcion.getTelefono() == null || suscripcion.getTelefono().isBlank()) {
            log.warn("[MensualidadVencimientoNotificationService] Suscripcion id={} placa={} no tiene telefono registrado, se omite alerta",
                suscripcion.getId(), suscripcion.getPlaca());
            return;
        }

        String mensaje = construirMensaje(suscripcion);
        boolean enviado = twilioService.enviarSms(suscripcion.getTelefono(), mensaje);

        if (!enviado) {
            log.warn("[MensualidadVencimientoNotificationService] No se pudo enviar alerta suscripcionId={} placa={}",
                suscripcion.getId(), suscripcion.getPlaca());
            return;
        }

        suscripcion.setAlertaVencimientoEnviada(true);
        suscripcionMensualRepositoryPort.save(suscripcion);
        log.info("[MensualidadVencimientoNotificationService] Alerta de vencimiento enviada suscripcionId={} placa={}",
            suscripcion.getId(), suscripcion.getPlaca());
    }

    private String construirMensaje(SuscripcionMensual suscripcion) {
        String nombreEmpresa = empresaRepositoryPort.findById(suscripcion.getEmpresaId())
            .map(Empresa::getNombre)
            .orElse("Tu parqueadero");
        String nombreSede = sedeRepositoryPort.findByIdAndEmpresaId(suscripcion.getSedeId(), suscripcion.getEmpresaId())
            .map(Sede::getNombre)
            .orElse("");

        return String.format(
            "%s %s%nTu mensualidad del vehiculo %s vence el %s. Renueva a tiempo para no perder el beneficio.",
            nombreEmpresa,
            nombreSede.isBlank() ? "" : "- " + nombreSede,
            suscripcion.getPlaca(),
            suscripcion.getFechaFin().format(DATE_FORMATTER)
        );
    }
}
