package saas.parqueadero.infrastructure.configuration.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del canal de sincronizacion con las instalaciones de escritorio.
 *
 * @param baseUrl                    URL publica del backend, para armar la {@code syncUrl}
 *                                   que se entrega al cliente. Si esta vacia se deriva de
 *                                   la peticion de alta.
 * @param retencionDias              dias que se conservan los snapshots historicos.
 * @param intervaloSugeridoSegundos  cada cuanto se le sugiere sincronizar a la instalacion.
 */
@ConfigurationProperties(prefix = "app.sync")
public record SyncProperties(String baseUrl, Integer retencionDias, Long intervaloSugeridoSegundos) {

    public SyncProperties {
        if (retencionDias == null || retencionDias < 1) {
            retencionDias = 90;
        }
        if (intervaloSugeridoSegundos == null || intervaloSugeridoSegundos < 60) {
            intervaloSugeridoSegundos = 600L;
        }
        if (baseUrl == null) {
            baseUrl = "";
        }
    }
}
