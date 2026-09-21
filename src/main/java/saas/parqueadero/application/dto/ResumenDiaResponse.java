package saas.parqueadero.application.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDiaResponse {
    private LocalDate fecha;
    private ConteoPorTipoResponse dentro;
    private ConteoPorTipoResponse entradasHoy;
    private ConteoPorTipoResponse salidasHoy;
}
