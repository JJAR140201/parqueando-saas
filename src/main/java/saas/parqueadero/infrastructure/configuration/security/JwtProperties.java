package saas.parqueadero.infrastructure.configuration.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Long expirationMillis, RefreshExpirationMillis refreshExpirationMillis) {

    private static final String INSECURE_DEFAULT = "CHANGE_THIS_SECRET_KEY_WITH_AT_LEAST_32_CHARS";
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "app.jwt.secret (variable de entorno APP_JWT_SECRET) es obligatorio y no esta configurado");
        }
        if (secret.equals(INSECURE_DEFAULT)) {
            throw new IllegalStateException(
                "app.jwt.secret esta usando el valor por defecto inseguro. Configura APP_JWT_SECRET con un secreto unico");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "app.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " caracteres para firmar con HMAC-SHA256");
        }
    }

    public record RefreshExpirationMillis(Long superAdmin, Long admin, Long operario) {
    }
}
