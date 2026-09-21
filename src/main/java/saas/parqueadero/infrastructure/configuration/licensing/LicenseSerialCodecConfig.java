package saas.parqueadero.infrastructure.configuration.licensing;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saas.parqueadero.licensing.LicenseSerialCodec;

@Configuration
@EnableConfigurationProperties(LicenseProperties.class)
@Slf4j
public class LicenseSerialCodecConfig {

    /** Debe coincidir con el default de {@code app.license.hmac-secret} en application.properties. */
    private static final String DEV_DEFAULT_SECRET = "Q0hBTkdFX1RISVNfTElDRU5TRV9TRUNSRVRfMzJCIQ==";

    @Bean
    public LicenseSerialCodec licenseSerialCodec(LicenseProperties properties) {
        // Ojo: si LICENSE_HMAC_SECRET existe en el entorno pero vacia (no ausente), el
        // placeholder ${LICENSE_HMAC_SECRET:default} de application.properties NO aplica el
        // default (Spring solo lo usa cuando la variable no existe). Por eso el fallback se
        // maneja aqui tambien, en vez de fallar duro como con app.jwt.secret.
        String secret = properties.hmacSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[LicenseSerialCodecConfig] app.license.hmac-secret no esta configurado, usando el secreto de "
                + "desarrollo por defecto. Configura la variable de entorno LICENSE_HMAC_SECRET antes de emitir licencias reales.");
            secret = DEV_DEFAULT_SECRET;
        } else if (DEV_DEFAULT_SECRET.equals(secret)) {
            log.warn("[LicenseSerialCodecConfig] Usando el secreto de licencia por defecto de desarrollo. "
                + "Configura la variable de entorno LICENSE_HMAC_SECRET antes de emitir licencias reales.");
        }
        return new LicenseSerialCodec(Base64.getDecoder().decode(secret));
    }
}
