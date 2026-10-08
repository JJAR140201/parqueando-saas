package saas.parqueadero.application.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;

/**
 * Arma las variables de cada plantilla de Meta. Las cuatro plantillas usan el mismo orden:
 * {{1}} cliente, {{2}} periodo, {{3}} valor, {{4}} fecha, {{5}} parqueadero. En
 * {@code payment_confirmation} la fecha es la del pago; en las demas, la de vencimiento.
 */
@Component
public class WhatsappMensajeFormatter {

    @SuppressWarnings("deprecation")
    private static final Locale ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_CO);
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES_CO);
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_CO);
    private static final int MAX_VARIABLE = 120;

    public List<String> variables(TipoNotificacionWhatsapp tipo, Cliente cliente, SuscripcionMensual suscripcion,
        Empresa empresa, LocalDate hoy) {
        LocalDate fecha = tipo == TipoNotificacionWhatsapp.CONFIRMACION_PAGO ? hoy : suscripcion.getFechaFin();
        return List.of(
            limpiar(cliente.getNombre()),
            periodo(suscripcion.getFechaInicio(), suscripcion.getFechaFin()),
            valor(suscripcion.getValorMensual()),
            FECHA.format(fecha),
            limpiar(empresa.getNombre())
        );
    }

    /** Variables de ejemplo para el mensaje de prueba. */
    public List<String> variablesDePrueba(String nombreEmpresa, LocalDate hoy) {
        return List.of("Cliente de prueba", MES_ANIO.format(hoy), "$1.000", FECHA.format(hoy.plusDays(5)), limpiar(nombreEmpresa));
    }

    static String periodo(LocalDate inicio, LocalDate fin) {
        if (inicio.getYear() == fin.getYear() && inicio.getMonth() == fin.getMonth()) {
            return MES_ANIO.format(inicio);
        }
        return "del " + DIA_MES.format(inicio) + " al " + FECHA.format(fin);
    }

    static String valor(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(ES_CO);
        simbolos.setGroupingSeparator('.');
        return "$" + new DecimalFormat("#,##0", simbolos).format(valor);
    }

    /** Meta rechaza saltos de linea, tabulaciones y mas de 4 espacios seguidos en las variables. */
    static String limpiar(String texto) {
        String limpio = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return "-";
        }
        return limpio.length() > MAX_VARIABLE ? limpio.substring(0, MAX_VARIABLE) : limpio;
    }
}
