package saas.parqueadero.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaRedemptionRequest {
    @NotBlank
    private String codigo;

    @Valid
    @NotNull
    private CreateEmpresaRequest empresa;

    @Valid
    @NotNull
    private LicenciaAdminRequest admin;
}
