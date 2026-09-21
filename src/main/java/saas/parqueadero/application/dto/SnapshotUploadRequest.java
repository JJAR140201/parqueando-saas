package saas.parqueadero.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuerpo que envia la instalacion de escritorio al sincronizar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotUploadRequest {

    /** Momento en que la instalacion genero el snapshot (reloj del cliente). */
    private Instant generadoEn;

    private String appVersion;

    private Licencia licencia;

    private Resumen resumen;

    /** Volcado completo de las tablas clave del escritorio. */
    @NotNull(message = "datos es obligatorio")
    private JsonNode datos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Licencia {
        private String estado;
        private LocalDate expiraEn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resumen {
        private Integer vehiculosDentro;
        private Integer entradasHoy;
        private Integer salidasHoy;
        private BigDecimal recaudoHoy;
    }
}
