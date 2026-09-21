package saas.parqueadero.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ultimo snapshot recibido de una instalacion, con el volcado completo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotDetalleResponse {
    private String tenantId;
    private Instant recibidoEn;
    private Instant generadoEn;
    private String appVersion;
    private String estadoLicencia;

    private Integer vehiculosDentro;
    private Integer entradasHoy;
    private Integer salidasHoy;
    private BigDecimal recaudoHoy;

    /** Volcado completo de las tablas del escritorio. */
    private JsonNode datos;
}
