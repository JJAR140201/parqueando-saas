package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertWhatsappCuentaRequest {
    @NotBlank
    @Size(max = 64)
    private String wabaId;

    @Size(max = 64)
    private String businessId;

    @NotBlank
    @Size(max = 64)
    private String phoneNumberId;

    @NotBlank
    @Size(max = 32)
    private String displayPhoneNumber;

    /** Obligatorio al crear; al actualizar, si va vacio se conserva el token actual. */
    @ToString.Exclude
    @Size(max = 1024)
    private String accessToken;

    private Boolean enabled;
}
