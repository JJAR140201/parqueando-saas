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
public class RegistroParqueoResponse {
    private Long id;
    private String placa;
    private TipoVehiculo tipoVehiculo;
    private LocalDateTime fechaEntrada;
    private LocalDateTime fechaSalida;
    private EstadoRegistroParqueo estado;
    private BigDecimal totalPagado;
    private Long sedeId;
    private Long usuarioId;
    private Long empresaId;
}
