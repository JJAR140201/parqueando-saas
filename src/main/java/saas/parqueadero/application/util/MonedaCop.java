package saas.parqueadero.application.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formato de pesos colombianos: sin decimales y con punto como separador de miles (ej. $12.500). */
public final class MonedaCop {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");

    private MonedaCop() {
    }

    public static String formatear(BigDecimal valor) {
        BigDecimal monto = valor == null ? BigDecimal.ZERO : valor.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(ES_CO);
        simbolos.setGroupingSeparator('.');
        // DecimalFormat no es thread-safe: se crea uno por llamada.
        return "$" + new DecimalFormat("#,##0", simbolos).format(monto);
    }
}
