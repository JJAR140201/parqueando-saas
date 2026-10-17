package saas.parqueadero.infrastructure.adapters.out.whatsapp;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.port.out.TokenCipherPort;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

/**
 * Cifra los tokens con AES-256-GCM. Formato guardado: {@code v1:} + Base64(iv de 12 bytes || cifrado+tag).
 * La clave llega por la variable de entorno WHATSAPP_ENCRYPTION_KEY (Base64 de 32 bytes).
 */
@Component
public class AesGcmTokenCipher implements TokenCipherPort {

    private static final String PREFIJO = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey clave;

    public AesGcmTokenCipher(WhatsappProperties properties) {
        this.clave = decodificarClave(properties.encryptionKey());
    }

    private static SecretKey decodificarClave(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            return bytes.length == 32 ? new SecretKeySpec(bytes, "AES") : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean estaConfigurado() {
        return clave != null;
    }

    @Override
    public String cifrar(String textoPlano) {
        exigirClave();
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return PREFIJO + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar el token", ex);
        }
    }

    @Override
    public String descifrar(String textoCifrado) {
        exigirClave();
        if (textoCifrado == null || !textoCifrado.startsWith(PREFIJO)) {
            throw new IllegalStateException("Token cifrado con un formato desconocido");
        }
        try {
            byte[] todo = Base64.getDecoder().decode(textoCifrado.substring(PREFIJO.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, todo, 0, IV_BYTES));
            return new String(cipher.doFinal(todo, IV_BYTES, todo.length - IV_BYTES), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // Mensaje generico: no se incluye nada del contenido cifrado
            throw new IllegalStateException("No se pudo descifrar el token (clave incorrecta o dato alterado)");
        }
    }

    private void exigirClave() {
        if (clave == null) {
            throw new BusinessException(
                "El cifrado de WhatsApp no esta configurado: define WHATSAPP_ENCRYPTION_KEY (Base64 de 32 bytes)");
        }
    }
}
