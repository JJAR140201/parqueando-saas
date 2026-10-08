package saas.parqueadero.domain.model;

/** Tipo de notificacion y plantilla de Meta con la que se envia. */
public enum TipoNotificacionWhatsapp {
    MENSUALIDAD_GENERADA("monthly_invoice_created"),
    RECORDATORIO_VENCIMIENTO("monthly_payment_reminder"),
    CONFIRMACION_PAGO("payment_confirmation"),
    MENSUALIDAD_VENCIDA("monthly_payment_overdue");

    private final String plantilla;

    TipoNotificacionWhatsapp(String plantilla) {
        this.plantilla = plantilla;
    }

    public String plantilla() {
        return plantilla;
    }
}
