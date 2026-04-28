package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoVehiculo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEntradaRequest {
    @NotBlank
    private String placa;

    @NotNull
    private TipoVehiculo tipoVehiculo;
}
