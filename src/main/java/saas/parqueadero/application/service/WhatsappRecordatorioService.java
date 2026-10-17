package saas.parqueadero.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.port.in.MensualidadNotificacionUseCase;
import saas.parqueadero.domain.port.in.MensualidadNotificacionUseCase.Resultado;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;

/** Envia diariamente el recordatorio de las mensualidades proximas a vencer. */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappRecordatorioService {

    private final SuscripcionMensualRepositoryPort suscripcionRepository;
    private final MensualidadNotificacionUseCase notificacionUseCase;
    private final Clock clock;

    @Value("${app.mensualidad.alerta.dias-anticipacion}")
    private int diasAnticipacion;

    @Scheduled(cron = "${app.whatsapp.recordatorio.cron}", zone = "America/Bogota")
    public void enviarRecordatorios() {
        try {
            LocalDate hoy = LocalDate.now(clock);
            List<SuscripcionMensual> pendientes =
                suscripcionRepository.findPendientesDeAlertaVencimiento(hoy, hoy.plusDays(diasAnticipacion));
            log.info("[WhatsappRecordatorioService] {} mensualidades pendientes de recordatorio", pendientes.size());

            for (SuscripcionMensual suscripcion : pendientes) {
                Resultado resultado = notificacionUseCase.notificar(
                    TipoNotificacionWhatsapp.RECORDATORIO_VENCIMIENTO, suscripcion.getId());
                // Sin cuenta de WhatsApp no se marca: si la empresa la configura despues, se le avisara
                if (resultado != Resultado.OMITIDA_SIN_CUENTA) {
                    suscripcion.setAlertaVencimientoEnviada(true);
                    suscripcionRepository.save(suscripcion);
                }
            }
        } catch (Exception ex) {
            log.error("[WhatsappRecordatorioService] Error enviando recordatorios", ex);
        }
    }
}
