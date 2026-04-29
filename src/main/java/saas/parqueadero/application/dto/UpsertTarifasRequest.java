package saas.parqueadero.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertTarifasRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal valorFraccionCarro;

    @NotNull
    @Min(1)
    private Integer minutosFraccionCarro;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal valorFraccionMoto;

    @NotNull
    @Min(1)
    private Integer minutosFraccionMoto;
}
