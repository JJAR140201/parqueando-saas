package saas.parqueadero.application.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMensajeResponse {
    private Long id;
    private Long empresaId;
    private Long clienteId;
    private Long suscripcionId;
    private String destination;
    private String messageType;
    private String templateName;
    private String providerMessageId;
    private String status;
    private Instant requestedAt;
    private Instant providerAcceptedAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant failedAt;
    private String errorCode;
    private String errorMessage;
    /** providerAcceptedAt - requestedAt, en milisegundos. */
    private Long latenciaApiMs;
    /** sentAt - requestedAt, en milisegundos. */
    private Long latenciaEnvioMs;
    /** deliveredAt - requestedAt, en milisegundos. */
    private Long latenciaEntregaMs;
}
