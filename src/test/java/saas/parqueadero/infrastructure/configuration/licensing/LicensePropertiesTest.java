package saas.parqueadero.infrastructure.configuration.licensing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class LicensePropertiesTest {

    @Test
    void rechazaVacioONulo() {
        assertThrows(IllegalStateException.class, () -> new LicenseProperties(null));
        assertThrows(IllegalStateException.class, () -> new LicenseProperties("  "));
    }

    @Test
    void rechazaElSecretoPorDefectoPublicado() {
        assertThrows(IllegalStateException.class,
            () -> new LicenseProperties("Q0hBTkdFX1RISVNfTElDRU5TRV9TRUNSRVRfMzJCIQ=="));
    }

    @Test
    void rechazaBase64InvalidoOCorto() {
        assertThrows(IllegalStateException.class, () -> new LicenseProperties("no es base64!!"));
        assertThrows(IllegalStateException.class,
            () -> new LicenseProperties(Base64.getEncoder().encodeToString(new byte[16])));
    }

    @Test
    void aceptaUnSecretoDe32Bytes() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        assertDoesNotThrow(() -> new LicenseProperties(Base64.getEncoder().encodeToString(bytes)));
    }
}
