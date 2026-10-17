package saas.parqueadero.domain.port.in;

import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;

public interface MensualidadNotificacionUseCase {

    enum Resultado {
        ENVIADA,
        OMITIDA_SIN_CUENTA,
        OMITIDA_NO_PERMITIDA,
        OMITIDA_TELEFONO_INVALIDO,
        FALLIDA
    }

    /** Intenta notificar al cliente de una mensualidad. Nunca lanza excepciones. */
    Resultado notificar(TipoNotificacionWhatsapp tipo, Long suscripcionId);
}
