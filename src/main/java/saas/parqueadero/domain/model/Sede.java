package saas.parqueadero.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sede {
    private Long id;
    private String nombre;
    private Integer capacidadTotal;
    private Integer capacidadActual;
    private Long empresaId;
}
