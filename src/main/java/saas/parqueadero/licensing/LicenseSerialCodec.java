package saas.parqueadero.licensing;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Genera y verifica el serial de licencia.
 *
 * <p>Estructura (18 bytes): {@code version(1) | expiraEpochDay(2 BE) | nonce(5) | hmac(10)}.
 * El HMAC-SHA256 se calcula sobre los primeros 8 bytes con la clave inyectada y se trunca a
 * 10 bytes. Se codifica en Crockford Base32 -> 29 caracteres, agrupados de 5:
 * {@code XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXX}.
 *
 * <p>A diferencia del serial de la app de escritorio (offline), este backend siempre esta en
 * linea: la firma solo protege la integridad/expiracion del codigo, el estado autoritativo
 * (pendiente/redimida/revocada) vive en base de datos.
 */
public final class LicenseSerialCodec {

    public static final int VERSION = 1;

    private static final int PLAIN_LEN = 8;
    private static final int TAG_LEN = 10;
    private static final int GROUP_SIZE = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long MAX_NONCE = (1L << 40) - 1;

    private final byte[] key;

    public LicenseSerialCodec(byte[] key) {
        this.key = key.clone();
    }

    /** Crea un serial nuevo, unico, que expira en {@code expiresAt}. */
    public String generate(LocalDate expiresAt) {
        long nonce = (RANDOM.nextLong() & MAX_NONCE);
        return generate(new LicenseSerialPayload(VERSION, expiresAt, nonce));
    }

    public String generate(LicenseSerialPayload payload) {
        long epochDay = payload.expiresAt().toEpochDay();
        if (epochDay < 0 || epochDay > 0xFFFF) {
            throw new IllegalArgumentException("Fecha de expiracion fuera de rango soportado");
        }
        byte[] plain = new byte[PLAIN_LEN];
        plain[0] = (byte) payload.version();
        plain[1] = (byte) ((epochDay >> 8) & 0xFF);
        plain[2] = (byte) (epochDay & 0xFF);
        byte[] nonceBytes = ByteBuffer.allocate(8).putLong(payload.nonce()).array();
        System.arraycopy(nonceBytes, 3, plain, 3, 5); // 5 bytes menos significativos

        byte[] tag = Arrays.copyOf(hmac(plain), TAG_LEN);
        byte[] serial = new byte[PLAIN_LEN + TAG_LEN];
        System.arraycopy(plain, 0, serial, 0, PLAIN_LEN);
        System.arraycopy(tag, 0, serial, PLAIN_LEN, TAG_LEN);

        return CrockfordBase32.group(CrockfordBase32.encode(serial), GROUP_SIZE);
    }

    /** Decodifica y verifica la firma. Lanza {@link LicenseSerialInvalidException} si algo no cuadra. */
    public LicenseSerialPayload verify(String serial) {
        if (serial == null || serial.isBlank()) {
            throw new LicenseSerialInvalidException("El codigo esta vacio");
        }
        byte[] bytes;
        try {
            bytes = CrockfordBase32.decode(serial);
        } catch (IllegalArgumentException ex) {
            throw new LicenseSerialInvalidException(ex.getMessage());
        }
        if (bytes.length < PLAIN_LEN + TAG_LEN) {
            throw new LicenseSerialInvalidException("El codigo esta incompleto");
        }
        byte[] plain = Arrays.copyOfRange(bytes, 0, PLAIN_LEN);
        byte[] tag = Arrays.copyOfRange(bytes, PLAIN_LEN, PLAIN_LEN + TAG_LEN);

        byte[] expected = Arrays.copyOf(hmac(plain), TAG_LEN);
        if (!java.security.MessageDigest.isEqual(expected, tag)) {
            throw new LicenseSerialInvalidException("El codigo no es valido (firma incorrecta)");
        }

        int version = plain[0] & 0xFF;
        int epochDay = ((plain[1] & 0xFF) << 8) | (plain[2] & 0xFF);
        long nonce = 0;
        for (int i = 3; i < PLAIN_LEN; i++) {
            nonce = (nonce << 8) | (plain[i] & 0xFF);
        }
        return new LicenseSerialPayload(version, LocalDate.ofEpochDay(epochDay), nonce);
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular HMAC", ex);
        }
    }
}
