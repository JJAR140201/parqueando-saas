package saas.parqueadero.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia (solo lectura) del estado operativo de una instalacion cliente en un
 * momento dado. El {@code payloadJson} trae el volcado completo de las tablas
 * clave del escritorio; el resto son campos de acceso rapido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotOperativo {

    private Long id;
    private String tenantId;

    /** Momento en que el servidor recibio el snapshot. */
    private Instant recibidoEn;

    /** Momento en que la instalacion genero el snapshot (reloj del cliente). */
    private Instant generadoEn;

    private String appVersion;
    private String estadoLicencia;

    private ResumenOperativo resumen;

    /** Volcado completo en JSON de las tablas del escritorio. */
    private String payloadJson;
}
