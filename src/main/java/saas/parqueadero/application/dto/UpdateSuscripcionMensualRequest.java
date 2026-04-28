package saas.parqueadero.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoVehiculo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSuscripcionMensualRequest {
    @NotNull
    private TipoVehiculo tipoVehiculo;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal valorMensual;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;

    private Boolean activa;

    private Long empresaId;

    private Long sedeId;
}
