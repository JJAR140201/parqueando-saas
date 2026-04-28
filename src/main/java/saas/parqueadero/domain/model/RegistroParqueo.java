package saas.parqueadero.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroParqueo {
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
