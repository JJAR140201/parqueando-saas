package saas.parqueadero.infrastructure.configuration.licensing;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.license")
public record LicenseProperties(String hmacSecret) {

    /** Secreto que estuvo publicado en el repositorio: cualquier despliegue que lo use es inseguro. */
    private static final String LEAKED_DEFAULT_SECRET = "Q0hBTkdFX1RISVNfTElDRU5TRV9TRUNSRVRfMzJCIQ==";
    private static final int MIN_SECRET_BYTES = 32;

    public LicenseProperties {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            throw new IllegalStateException(
                "app.license.hmac-secret (variable de entorno LICENSE_HMAC_SECRET) es obligatorio y no esta configurado");
        }
        if (LEAKED_DEFAULT_SECRET.equals(hmacSecret.trim())) {
            throw new IllegalStateException(
                "app.license.hmac-secret usa el secreto por defecto, que es publico. Configura LICENSE_HMAC_SECRET con un secreto unico");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(hmacSecret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.license.hmac-secret debe estar codificado en Base64", ex);
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "app.license.hmac-secret debe tener al menos " + MIN_SECRET_BYTES + " bytes (despues de decodificar Base64)");
        }
    }
}
