package saas.parqueadero.licensing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class LicenseSerialCodecTest {

    private final LicenseSerialCodec codec = new LicenseSerialCodec(
        Base64.getDecoder().decode("Q0hBTkdFX1RISVNfTElDRU5TRV9TRUNSRVRfMzJCIQ=="));

    @Test
    void generaYVerificaUnSerialValido() {
        LocalDate expiracion = LocalDate.now().plusDays(365);
        String serial = codec.generate(expiracion);

        LicenseSerialPayload payload = codec.verify(serial);

        assertThat(payload.expiresAt()).isEqualTo(expiracion);
        assertThat(payload.version()).isEqualTo(LicenseSerialCodec.VERSION);
        assertThat(payload.estaVencida(LocalDate.now())).isFalse();
    }

    @Test
    void detectaSerialVencido() {
        LocalDate expiracion = LocalDate.now().minusDays(1);
        String serial = codec.generate(expiracion);

        LicenseSerialPayload payload = codec.verify(serial);

        assertThat(payload.estaVencida(LocalDate.now())).isTrue();
    }

    @Test
    void rechazaSerialAlterado() {
        String serial = codec.generate(LocalDate.now().plusDays(365));
        String alterado = serial.substring(0, serial.length() - 1)
            + (serial.charAt(serial.length() - 1) == '0' ? '1' : '0');

        assertThatThrownBy(() -> codec.verify(alterado)).isInstanceOf(LicenseSerialInvalidException.class);
    }

    @Test
    void rechazaSerialVacio() {
        assertThatThrownBy(() -> codec.verify("")).isInstanceOf(LicenseSerialInvalidException.class);
    }

    @Test
    void rechazaFirmaConOtraClave() {
        String serial = codec.generate(LocalDate.now().plusDays(365));
        LicenseSerialCodec otraClave = new LicenseSerialCodec(
            Base64.getDecoder().decode("VE9UQUxNRU5URV9ESUZFUkVOVEVfMzJCWVRFUyEhISE="));

        assertThatThrownBy(() -> otraClave.verify(serial)).isInstanceOf(LicenseSerialInvalidException.class);
    }
}
