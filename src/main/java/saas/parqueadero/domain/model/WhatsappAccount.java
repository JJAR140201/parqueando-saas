package saas.parqueadero.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Cuenta de WhatsApp Business (Meta Cloud API) de una empresa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappAccount {
    private Long id;
    private Long empresaId;
    private String provider;
    private String wabaId;
    private String businessId;
    private String phoneNumberId;
    private String displayPhoneNumber;
    /** Token de acceso cifrado. Nunca se registra en logs ni se expone. */
    @ToString.Exclude
    private String accessTokenEncrypted;
    private WhatsappAccountStatus status;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
