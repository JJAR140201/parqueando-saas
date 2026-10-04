package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensualidadVencimientoNotificationService {

    private final SuscripcionMensualRepositoryPort suscripcionMensualRepositoryPort;

    /**
     * Cancela automaticamente las mensualidades activas cuya fechaFin ya paso. Si el cliente
     * paga y renueva, la reactivacion es manual (editar fechas + marcar activa) desde la UI.
     */
    @Scheduled(cron = "${app.mensualidad.cancelacion.cron}", zone = "America/Bogota")
    public void cancelarVencidas() {
        LocalDate hoy = LocalDate.now();
        List<SuscripcionMensual> vencidas = suscripcionMensualRepositoryPort.findActivasVencidas(hoy);

        log.info("[MensualidadVencimientoNotificationService] {} mensualidades vencidas para cancelar automaticamente", vencidas.size());

        vencidas.forEach(suscripcion -> {
            suscripcion.setActiva(false);
            suscripcionMensualRepositoryPort.save(suscripcion);
            log.info("[MensualidadVencimientoNotificationService] Mensualidad cancelada automaticamente por vencimiento id={} placa={} fechaFin={}",
                suscripcion.getId(), suscripcion.getPlaca(), suscripcion.getFechaFin());
        });
    }
}
