package saas.parqueadero.infrastructure.adapters.out.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import saas.parqueadero.domain.port.in.MensualidadNotificacionUseCase;
import saas.parqueadero.infrastructure.adapters.out.event.SpringMensualidadEventosAdapter.MensualidadEvento;

/**
 * Recibe los eventos de mensualidades despues de que la transaccion que los origino se confirmo, y
 * envia la notificacion en un hilo aparte. Si no hay transaccion activa, se ejecuta de inmediato.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MensualidadEventoListener {

    private final MensualidadNotificacionUseCase notificacionUseCase;

    @Async("whatsappExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRecibirEvento(MensualidadEvento evento) {
        try {
            notificacionUseCase.notificar(evento.tipo(), evento.suscripcionId());
        } catch (Exception ex) {
            // Un fallo de notificacion nunca debe propagarse al proceso de negocio
            log.error("[MensualidadEventoListener] Error inesperado notificando {} de la mensualidad {}",
                evento.tipo(), evento.suscripcionId(), ex);
        }
    }
}
