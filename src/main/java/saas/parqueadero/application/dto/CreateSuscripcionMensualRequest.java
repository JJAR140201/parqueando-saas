package saas.parqueadero.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateSuscripcionMensualRequest {
    @NotBlank
    private String placa;

    @NotNull
    private TipoVehiculo tipoVehiculo;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal valorMensual;

    @NotNull
    private LocalDate fechaInicio;

    @NotNull
    private LocalDate fechaFin;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "El telefono debe estar en formato E.164 (ej: +573001234567)")
    private String telefono;

    private Long empresaId;

    private Long sedeId;
}
