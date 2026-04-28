package saas.parqueadero.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuscripcionMensual {
    private Long id;
    private String placa;
    private TipoVehiculo tipoVehiculo;
    private BigDecimal valorMensual;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Boolean activa;
    private Long sedeId;
    private Long empresaId;
}
