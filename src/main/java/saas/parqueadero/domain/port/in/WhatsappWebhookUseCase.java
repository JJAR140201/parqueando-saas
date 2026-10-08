package saas.parqueadero.domain.port.in;

public interface WhatsappWebhookUseCase {

    /** Valida la firma {@code X-Hub-Signature-256} de Meta sobre el cuerpo crudo. */
    boolean firmaValida(byte[] cuerpo, String cabeceraFirma);

    /**
     * Verificacion de la URL del webhook ({@code hub.mode}, {@code hub.verify_token}, {@code hub.challenge}).
     *
     * @return el desafio a devolver a Meta, o null si la verificacion falla
     */
    String verificarSuscripcion(String modo, String token, String desafio);

    /** Actualiza los estados de los mensajes que informa Meta. */
    void procesar(byte[] cuerpo);
}
