package saas.parqueadero.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Registro de un mensaje de WhatsApp y de su recorrido hasta la entrega. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMessage {
    private Long id;
    private Long empresaId;
    private Long whatsappAccountId;
    private Long clienteId;
    private Long suscripcionId;
    private String destination;
    private TipoNotificacionWhatsapp messageType;
    private String templateName;
    /** Variables de la plantilla, en JSON, para auditar y reintentar. */
    private String templateVariables;
    private String providerMessageId;
    private WhatsappMessageStatus status;
    private Instant requestedAt;
    private Instant providerAcceptedAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant failedAt;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
}
