package saas.parqueadero.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoVehiculo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrecioSalidaResponse {
    private String placa;
    private TipoVehiculo tipoVehiculo;
    private String tipo;
    private LocalDateTime fechaEntrada;
    private LocalDateTime fechaSalida;
    private Long minutosEstadia;
    private BigDecimal horas;
    private BigDecimal totalPagado;
    private BigDecimal total;
    private Long sedeId;
    private Long empresaId;
}