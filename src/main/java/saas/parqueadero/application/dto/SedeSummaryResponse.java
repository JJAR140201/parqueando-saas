package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeSummaryResponse {
    private Long id;
    private String nombre;
    private Integer capacidadTotal;
    private Integer capacidadActual;
}
