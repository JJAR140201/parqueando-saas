package saas.parqueadero.infrastructure.configuration.licensing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.license")
public record LicenseProperties(String hmacSecret) {
}
