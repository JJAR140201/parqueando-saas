package saas.parqueadero.application.dto;

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
public class SuscripcionMensualResponse {
    private Long id;
    private String placa;
    private TipoVehiculo tipoVehiculo;
    private BigDecimal valorMensual;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activa;
    private Boolean vigenteHoy;
    private Long sedeId;
    private Long empresaId;
}
