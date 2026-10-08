package saas.parqueadero.domain.port.out;

import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;

/**
 * Publica hechos de las mensualidades que pueden disparar notificaciones. La entrega es
 * posterior a la transaccion que los origino y no puede hacerla fallar.
 */
public interface MensualidadEventosPort {
    void publicar(TipoNotificacionWhatsapp tipo, Long suscripcionId);
}
