package saas.parqueadero.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
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
public class CreateEmpresaSedeRequest {
    @NotBlank
    private String nombre;

    @JsonAlias("capacidad")
    @NotNull
    @Min(1)
    private Integer capacidadTotal;
}
