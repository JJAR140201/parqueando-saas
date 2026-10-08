package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessageStatus;

@Entity
@Table(name = "whatsapp_message", indexes = {
    @Index(name = "idx_whatsapp_message_empresa_fecha", columnList = "empresa_id, requested_at"),
    @Index(name = "idx_whatsapp_message_suscripcion", columnList = "suscripcion_id, message_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "whatsapp_account_id")
    private Long whatsappAccountId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "suscripcion_id")
    private Long suscripcionId;

    @Column(nullable = false, length = 32)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 40)
    private TipoNotificacionWhatsapp messageType;

    @Column(name = "template_name", nullable = false, length = 80)
    private String templateName;

    @Column(name = "template_variables", columnDefinition = "text")
    private String templateVariables;

    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WhatsappMessageStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "provider_accepted_at")
    private Instant providerAcceptedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
