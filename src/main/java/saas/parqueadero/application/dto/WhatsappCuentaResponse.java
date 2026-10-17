package saas.parqueadero.application.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configuracion de WhatsApp de una empresa. El token de acceso nunca se devuelve. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappCuentaResponse {
    private Long id;
    private Long empresaId;
    private String provider;
    private String wabaId;
    private String businessId;
    private String phoneNumberId;
    private String displayPhoneNumber;
    private String status;
    private boolean enabled;
    /** Indica si hay un token guardado, sin revelarlo. */
    private boolean tokenConfigurado;
    private Instant createdAt;
    private Instant updatedAt;
}
