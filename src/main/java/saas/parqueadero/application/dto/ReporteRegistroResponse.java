package saas.parqueadero.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.TipoVehiculo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteRegistroResponse {

    private Long id;
    private String placa;
    private TipoVehiculo tipoVehiculo;
    private LocalDateTime fechaEntrada;
    private LocalDateTime fechaSalida;
    /** Minutos de estadía, nulo si el vehículo sigue activo */
    private Long minutosEstadia;
    private BigDecimal totalPagado;
    private EstadoRegistroParqueo estado;
    private Long sedeId;
    private Long usuarioId;
}
