package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import saas.parqueadero.domain.model.WhatsappAccountStatus;

@Entity
@Table(name = "whatsapp_account", uniqueConstraints = {
    @UniqueConstraint(name = "uk_whatsapp_account_empresa", columnNames = {"empresa_id"}),
    @UniqueConstraint(name = "uk_whatsapp_account_phone", columnNames = {"phone_number_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "waba_id", nullable = false, length = 64)
    private String wabaId;

    @Column(name = "business_id", length = 64)
    private String businessId;

    @Column(name = "phone_number_id", nullable = false, length = 64)
    private String phoneNumberId;

    @Column(name = "display_phone_number", nullable = false, length = 32)
    private String displayPhoneNumber;

    @ToString.Exclude
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "text")
    private String accessTokenEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WhatsappAccountStatus status;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
