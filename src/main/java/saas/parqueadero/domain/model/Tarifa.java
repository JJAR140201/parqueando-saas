package saas.parqueadero.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {
    private Long id;
    private TipoVehiculo tipoVehiculo;
    private BigDecimal valorFraccion;
    private Integer minutosFraccion;
    private Long sedeId;
    private Long empresaId;
}
