package saas.parqueadero.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cifras rapidas del dia que cada instalacion cliente reporta en su snapshot.
 * Se guardan denormalizadas para pintar el tablero del proveedor sin abrir el
 * payload JSON completo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenOperativo {
    private Integer vehiculosDentro;
    private Integer entradasHoy;
    private Integer salidasHoy;
    private BigDecimal recaudoHoy;
}
