package saas.parqueadero.domain.model;

/** Ciclo de vida de un mensaje. El orden de los estados "avanzados" es el de la entrega. */
public enum WhatsappMessageStatus {
    /** El mensaje no se envio (telefono invalido, sin consentimiento, etc.). */
    NOT_SENT(0),
    REQUESTED(1),
    ACCEPTED(2),
    SENT(3),
    DELIVERED(4),
    READ(5),
    FAILED(6);

    private final int rango;

    WhatsappMessageStatus(int rango) {
        this.rango = rango;
    }

    public int rango() {
        return rango;
    }

    public boolean esEntregaAvanzada() {
        return this == DELIVERED || this == READ;
    }
}
