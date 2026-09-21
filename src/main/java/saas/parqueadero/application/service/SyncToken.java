package saas.parqueadero.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de sincronizacion de una instalacion: aleatorio, opaco y sin estado.
 * El servidor solo guarda su hash SHA-256; el token en claro se entrega una vez.
 */
public final class SyncToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private SyncToken() {
    }

    /** Genera un token nuevo (43 caracteres, 32 bytes de entropia). */
    public static String generar() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return B64.encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal (64 caracteres) del token. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    /** Comparacion en tiempo constante de dos hashes hex. */
    public static boolean hashesCoinciden(String hashA, String hashB) {
        if (hashA == null || hashB == null) {
            return false;
        }
        return MessageDigest.isEqual(
            hashA.getBytes(StandardCharsets.UTF_8),
            hashB.getBytes(StandardCharsets.UTF_8));
    }
}
