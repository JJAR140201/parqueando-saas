package saas.parqueadero.application.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import saas.parqueadero.application.util.MonedaCop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saas.parqueadero.application.dto.PrecioSalidaResponse;

/**
 * Genera un ticket/recibo de parqueo en formato PDF imprimible.
 * El ticket es un recibo angosto estilo termico (58 mm de ancho, alto segun el contenido).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketParqueoService {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final float TICKET_WIDTH  = 164f;   // ~58 mm en puntos
    private static final float MEASURE_HEIGHT = 1000f; // pagina de medicion; el alto real se ajusta al contenido
    private static final float MARGIN        = 8f;
    private static final int   SEPARATOR_LEN = 30;
    private static final int   LABEL_WIDTH   = 10;
    private static final float LEADING       = 1.15f;

    // Fuentes
    private static final Font FONT_TITLE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, Color.BLACK);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8, Color.BLACK);
    private static final Font FONT_BODY   = FontFactory.getFont(FontFactory.HELVETICA,       7,  Color.BLACK);
    private static final Font FONT_SMALL  = FontFactory.getFont(FontFactory.HELVETICA,       6,  Color.DARK_GRAY);
    private static final Font FONT_TOTAL  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, Color.BLACK);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  6.5f, Color.BLACK);

    /**
     * Genera el PDF del ticket a partir de un {@link PrecioSalidaResponse}.
     *
     * @param precio datos del calculo de salida
     * @return bytes del PDF generado
     */
    public byte[] generarTicket(PrecioSalidaResponse precio) {
        log.debug("[TicketParqueoService] Generando ticket para placa={}", precio.getPlaca());

        // Dos pasadas: la primera mide cuanto ocupa el contenido; la segunda ajusta la pagina a ese alto.
        Render medido = renderizar(precio, MEASURE_HEIGHT);
        return renderizar(precio, medido.usado() + MARGIN).pdf();
    }

    private record Render(byte[] pdf, float usado) {}

    private Render renderizar(PrecioSalidaResponse precio, float alto) {
        Rectangle pageSize = new Rectangle(TICKET_WIDTH, alto);
        pageSize.setBackgroundColor(Color.WHITE);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(pageSize, MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph empresa = parrafo(valor(precio.getNombreEmpresa()), FONT_TITLE);
            empresa.setAlignment(Element.ALIGN_CENTER);
            empresa.setSpacingAfter(2f);
            doc.add(empresa);

            Paragraph sede = parrafo(valor(precio.getNombreSede()), FONT_HEADER);
            sede.setAlignment(Element.ALIGN_CENTER);
            sede.setSpacingAfter(6f);
            doc.add(sede);

            // ---- Titulo ----
            Paragraph titulo = parrafo("RECIBO DE PARQUEO", FONT_TITLE);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(4f);
            doc.add(titulo);

            // ---- Separador linea ----
            doc.add(separador('='));

            // ---- Fecha ----
            String fecha = precio.getFechaEntrada() != null
                ? precio.getFechaEntrada().format(DATE_FMT)
                : "N/A";

            Paragraph fechaPar = parrafo(fecha, FONT_HEADER);
            fechaPar.setAlignment(Element.ALIGN_CENTER);
            fechaPar.setSpacingAfter(6f);
            doc.add(fechaPar);

            addLabelValue(doc, "CAJERO:", valor(precio.getNombreUsuario()));
            addLabelValue(doc, "METODO:", valor(precio.getMetodoPago()));

            doc.add(espaciado(2f));

            // ---- Placa ----
            addLabelValue(doc, "PLACA:", precio.getPlaca() != null ? precio.getPlaca() : "N/A");

            // ---- Tipo Vehiculo ----
            String tipo = precio.getTipoVehiculo() != null ? precio.getTipoVehiculo().name() : "N/A";
            addLabelValue(doc, "TIPO:", tipo);

            doc.add(espaciado(4f));

            // ---- Horarios ----
            String horaEntrada = precio.getFechaEntrada() != null
                ? precio.getFechaEntrada().format(TIME_FMT)
                : "N/A";
            String horaSalida = precio.getFechaSalida() != null
                ? precio.getFechaSalida().format(TIME_FMT)
                : "N/A";

            addLabelValue(doc, "DESDE:", horaEntrada);
            addLabelValue(doc, "HASTA:", horaSalida);

            // ---- Duracion ----
            if (precio.getHoras() != null) {
                addLabelValue(doc, "DURACION:", precio.getHoras() + " horas");
            }

            if (precio.getFechaSalida() != null) {
                addLabelValue(doc, "FECHA SAL:", precio.getFechaSalida().format(DATE_FMT));
            }

            // ---- Separador punteado ----
            doc.add(espaciado(4f));
            doc.add(separador('.'));
            doc.add(espaciado(4f));

            // ---- Total ----
            boolean mensualidad = Boolean.TRUE.equals(precio.getMensualidadActiva());
            String valorLabel = mensualidad ? "SIN COSTO (MENSUALIDAD)" : formatearValor(precio);

            Paragraph totalPar = parrafo("PAGADO: " + valorLabel, FONT_TOTAL);
            totalPar.setAlignment(Element.ALIGN_CENTER);
            totalPar.setSpacingAfter(2f);
            doc.add(totalPar);

            if (mensualidad) {
                Paragraph mensualPar = parrafo("Mensualidad activa", FONT_SMALL);
                mensualPar.setAlignment(Element.ALIGN_CENTER);
                doc.add(mensualPar);
            }

            // ---- Separador punteado inferior ----
            doc.add(espaciado(6f));
            doc.add(separador('.'));
            doc.add(espaciado(6f));

            // ---- Codigo de referencia (placa como texto) ----
            if (precio.getPlaca() != null) {
                Paragraph refPar = parrafo("REF: " + precio.getPlaca(), FONT_SMALL);
                refPar.setAlignment(Element.ALIGN_CENTER);
                refPar.setSpacingAfter(6f);
                doc.add(refPar);
            }

            // ---- Mensaje final ----
            Paragraph gracias = parrafo("GRACIAS POR SU VISITA", FONT_FOOTER);
            gracias.setAlignment(Element.ALIGN_CENTER);
            doc.add(gracias);

            Paragraph maneje = parrafo("MANEJE CON PRECAUCION", FONT_FOOTER);
            maneje.setAlignment(Element.ALIGN_CENTER);
            doc.add(maneje);

            float usado = alto - writer.getVerticalPosition(true) + MARGIN;
            doc.close();
            log.debug("[TicketParqueoService] Ticket PDF generado correctamente para placa={}", precio.getPlaca());
            return new Render(out.toByteArray(), usado);
        } catch (DocumentException | java.io.IOException e) {
            log.error("[TicketParqueoService] Error generando ticket PDF", e);
            throw new RuntimeException("No se pudo generar el ticket de parqueo", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Parrafo con interlineado compacto (el de iText/OpenPDF es 1.5x y alarga mucho el ticket). */
    private Paragraph parrafo(String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setLeading(0f, LEADING);
        return p;
    }

    private Paragraph parrafo() {
        Paragraph p = new Paragraph();
        p.setLeading(0f, LEADING);
        return p;
    }

    private Paragraph parrafo(String texto) {
        return parrafo(texto, FONT_BODY);
    }

    private void addLabelValue(Document doc, String label, String value) throws DocumentException {
        Paragraph p = parrafo();
        p.add(new Chunk(padRight(label, LABEL_WIDTH), FONT_BODY));
        p.add(new Chunk(value, FONT_HEADER));
        p.setSpacingAfter(2f);
        doc.add(p);
    }

    private Paragraph separador(char car) {
        String linea = String.valueOf(car).repeat(SEPARATOR_LEN);
        Paragraph p = parrafo(linea, FONT_SMALL);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(2f);
        return p;
    }

    private Paragraph espaciado(float espacio) {
        Paragraph p = parrafo(" ", FONT_SMALL);
        p.setSpacingAfter(espacio);
        return p;
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private String formatearValor(PrecioSalidaResponse precio) {
        if (precio.getTotalPagado() != null) {
            return MonedaCop.formatear(precio.getTotalPagado());
        }
        return MonedaCop.formatear(precio.getTotal());
    }

    private String valor(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }
}
