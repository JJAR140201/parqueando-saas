package saas.parqueadero.licensing;

/**
 * Crockford Base32: pensado para codigos escritos a mano. El alfabeto evita
 * caracteres ambiguos (I, L, O, U) y al decodificar tolera minusculas, guiones,
 * espacios y confusiones tipicas (I/L -> 1, O -> 0).
 */
public final class CrockfordBase32 {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int[] LOOKUP = new int[128];

    static {
        java.util.Arrays.fill(LOOKUP, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            LOOKUP[ALPHABET[i]] = i;
        }
        LOOKUP['I'] = 1;
        LOOKUP['L'] = 1;
        LOOKUP['O'] = 0;
    }

    private CrockfordBase32() {
    }

    public static String encode(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(ALPHABET[(buffer >> bitsLeft) & 0x1F]);
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
        }
        return sb.toString();
    }

    public static byte[] decode(String text) {
        String cleaned = text.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(cleaned.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            int value = c < 128 ? LOOKUP[c] : -1;
            if (value < 0) {
                throw new IllegalArgumentException("Caracter no valido en el codigo: '" + c + "'");
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out.write((buffer >> bitsLeft) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    /** Inserta un guion cada {@code groupSize} caracteres para facilitar la lectura. */
    public static String group(String code, int groupSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            if (i > 0 && i % groupSize == 0) {
                sb.append('-');
            }
            sb.append(code.charAt(i));
        }
        return sb.toString();
    }
}
