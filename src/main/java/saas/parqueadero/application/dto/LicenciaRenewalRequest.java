package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Renovacion de la licencia de una empresa existente, autenticada con las credenciales de su ADMIN. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaRenewalRequest {
    @NotBlank
    private String codigo;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
