package saas.parqueadero.infrastructure.adapters.out.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.port.out.WhatsappGateway;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

class AesGcmTokenCipherTest {

    private static WhatsappProperties conClave(String clave) {
        return new WhatsappProperties(null, null, null, null, null, clave, null, null);
    }

    private static String claveAleatoria() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void cifraYDescifraElMismoTexto() {
        AesGcmTokenCipher cipher = new AesGcmTokenCipher(conClave(claveAleatoria()));

        String cifrado = cipher.cifrar("EAAG-token-secreto");

        assertThat(cifrado).startsWith("v1:").doesNotContain("EAAG-token-secreto");
        assertThat(cipher.descifrar(cifrado)).isEqualTo("EAAG-token-secreto");
    }

    @Test
    void cadaCifradoUsaUnVectorDistinto() {
        AesGcmTokenCipher cipher = new AesGcmTokenCipher(conClave(claveAleatoria()));

        assertThat(cipher.cifrar("mismo")).isNotEqualTo(cipher.cifrar("mismo"));
    }

    @Test
    void otraClaveNoPuedeDescifrar() {
        String cifrado = new AesGcmTokenCipher(conClave(claveAleatoria())).cifrar("secreto");
        AesGcmTokenCipher otra = new AesGcmTokenCipher(conClave(claveAleatoria()));

        assertThatThrownBy(() -> otra.descifrar(cifrado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detectaDatosAlterados() {
        AesGcmTokenCipher cipher = new AesGcmTokenCipher(conClave(claveAleatoria()));
        String cifrado = cipher.cifrar("secreto");
        String alterado = cifrado.substring(0, cifrado.length() - 4) + (cifrado.endsWith("AAAA") ? "BBBB" : "AAAA");

        assertThatThrownBy(() -> cipher.descifrar(alterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinClaveValidaNoCifraNiDescifra() {
        for (String clave : new String[] {null, "", "no-es-base64!!", Base64.getEncoder().encodeToString(new byte[16])}) {
            AesGcmTokenCipher cipher = new AesGcmTokenCipher(conClave(clave));

            assertThat(cipher.estaConfigurado()).isFalse();
            assertThatThrownBy(() -> cipher.cifrar("x")).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void lasCredencialesNoMuestranElTokenEnToString() {
        WhatsappGateway.Credenciales credenciales = new WhatsappGateway.Credenciales("123", "EAAG-token-secreto");

        assertThat(credenciales.toString()).doesNotContain("EAAG-token-secreto").contains("123");
    }
}
