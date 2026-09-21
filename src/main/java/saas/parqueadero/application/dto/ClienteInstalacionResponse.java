package saas.parqueadero.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista de una instalacion cliente para la consola del proveedor (sin el token). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInstalacionResponse {
    private String tenantId;
    private String nombre;
    private Long empresaId;
    private boolean activa;
    private Instant creadaEn;
    private Instant ultimaSyncEn;
    private String appVersion;
    private String estadoLicencia;
    private LocalDate licenciaExpiraEn;

    private Integer vehiculosDentro;
    private Integer entradasHoy;
    private Integer salidasHoy;
    private BigDecimal recaudoHoy;
}
