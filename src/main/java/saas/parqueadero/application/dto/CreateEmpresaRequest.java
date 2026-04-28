package saas.parqueadero.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmpresaRequest {
    @NotBlank
    private String nit;

    @NotBlank
    private String nombre;

    @Valid
    @NotEmpty
    private List<CreateEmpresaSedeRequest> sedes;
}
