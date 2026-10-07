package saas.parqueadero.infrastructure.configuration.licensing;

import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import saas.parqueadero.licensing.LicenseSerialCodec;

@Configuration
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseSerialCodecConfig {

    @Bean
    public LicenseSerialCodec licenseSerialCodec(LicenseProperties properties) {
        return new LicenseSerialCodec(Base64.getDecoder().decode(properties.hmacSecret().trim()));
    }
}
