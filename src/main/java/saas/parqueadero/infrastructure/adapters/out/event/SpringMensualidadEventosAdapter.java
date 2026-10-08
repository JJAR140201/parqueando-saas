package saas.parqueadero.infrastructure.adapters.out.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.port.out.MensualidadEventosPort;

@Component
@RequiredArgsConstructor
public class SpringMensualidadEventosAdapter implements MensualidadEventosPort {

    /** Hecho de una mensualidad que puede disparar una notificacion. */
    public record MensualidadEvento(TipoNotificacionWhatsapp tipo, Long suscripcionId) {
    }

    private final ApplicationEventPublisher publisher;

    @Override
    public void publicar(TipoNotificacionWhatsapp tipo, Long suscripcionId) {
        publisher.publishEvent(new MensualidadEvento(tipo, suscripcionId));
    }
}
