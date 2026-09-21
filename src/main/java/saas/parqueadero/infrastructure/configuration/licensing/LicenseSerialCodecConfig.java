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
        if (properties.hmacSecret() == null || properties.hmacSecret().isBlank()) {
            throw new IllegalStateException(
                "app.license.hmac-secret (variable de entorno LICENSE_HMAC_SECRET) es obligatorio");
        }
        if (DEV_DEFAULT_SECRET.equals(properties.hmacSecret())) {
            log.warn("[LicenseSerialCodecConfig] Usando el secreto de licencia por defecto de desarrollo. "
                + "Configura la variable de entorno LICENSE_HMAC_SECRET antes de emitir licencias reales.");
        }
        return new LicenseSerialCodec(Base64.getDecoder().decode(properties.hmacSecret()));
    }
}
