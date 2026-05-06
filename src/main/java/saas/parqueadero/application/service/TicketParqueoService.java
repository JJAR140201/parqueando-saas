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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saas.parqueadero.application.dto.PrecioSalidaResponse;

/**
 * Genera un ticket/recibo de parqueo en formato PDF imprimible.
 * El ticket es un recibo angosto estilo termico (80 mm de ancho).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketParqueoService {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final float TICKET_WIDTH  = 226f;   // ~80 mm en puntos
    private static final float TICKET_HEIGHT = 620f;
    private static final float MARGIN        = 14f;

    // Fuentes
    private static final Font FONT_TITLE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  16, Color.BLACK);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, Color.BLACK);
    private static final Font FONT_BODY   = FontFactory.getFont(FontFactory.HELVETICA,       9,  Color.BLACK);
    private static final Font FONT_SMALL  = FontFactory.getFont(FontFactory.HELVETICA,       7,  Color.DARK_GRAY);
    private static final Font FONT_TOTAL  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, Color.BLACK);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8,  Color.BLACK);

    /**
     * Genera el PDF del ticket a partir de un {@link PrecioSalidaResponse}.
     *
     * @param precio datos del calculo de salida
     * @return bytes del PDF generado
     */
    public byte[] generarTicket(PrecioSalidaResponse precio) {
        log.info("[TicketParqueoService] Generando ticket para placa={}", precio.getPlaca());

        Rectangle pageSize = new Rectangle(TICKET_WIDTH, TICKET_HEIGHT);
        pageSize.setBackgroundColor(Color.WHITE);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(pageSize, MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph empresa = new Paragraph(valor(precio.getNombreEmpresa()), FONT_TITLE);
            empresa.setAlignment(Element.ALIGN_CENTER);
            empresa.setSpacingAfter(2f);
            doc.add(empresa);

            Paragraph sede = new Paragraph(valor(precio.getNombreSede()), FONT_HEADER);
            sede.setAlignment(Element.ALIGN_CENTER);
            sede.setSpacingAfter(6f);
            doc.add(sede);

            // ---- Titulo ----
            Paragraph titulo = new Paragraph("RECIBO DE PARQUEO", FONT_TITLE);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(4f);
            doc.add(titulo);

            // ---- Separador linea ----
            doc.add(separador('='));

            // ---- Fecha ----
            String fecha = precio.getFechaEntrada() != null
                ? precio.getFechaEntrada().format(DATE_FMT)
                : "N/A";

            Paragraph fechaPar = new Paragraph(fecha, FONT_HEADER);
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

            Paragraph totalPar = new Paragraph("PAGADO: " + valorLabel, FONT_TOTAL);
            totalPar.setAlignment(Element.ALIGN_CENTER);
            totalPar.setSpacingAfter(2f);
            doc.add(totalPar);

            if (mensualidad) {
                Paragraph mensualPar = new Paragraph("Mensualidad activa", FONT_SMALL);
                mensualPar.setAlignment(Element.ALIGN_CENTER);
                doc.add(mensualPar);
            }

            // ---- Separador punteado inferior ----
            doc.add(espaciado(6f));
            doc.add(separador('.'));
            doc.add(espaciado(6f));

            // ---- Codigo de referencia (placa como texto) ----
            if (precio.getPlaca() != null) {
                Paragraph refPar = new Paragraph("REF: " + precio.getPlaca(), FONT_SMALL);
                refPar.setAlignment(Element.ALIGN_CENTER);
                refPar.setSpacingAfter(6f);
                doc.add(refPar);
            }

            // ---- Mensaje final ----
            Paragraph gracias = new Paragraph("GRACIAS POR SU VISITA", FONT_FOOTER);
            gracias.setAlignment(Element.ALIGN_CENTER);
            doc.add(gracias);

            Paragraph maneje = new Paragraph("MANEJE CON PRECAUCION", FONT_FOOTER);
            maneje.setAlignment(Element.ALIGN_CENTER);
            doc.add(maneje);

            doc.close();
            log.info("[TicketParqueoService] Ticket PDF generado correctamente para placa={}", precio.getPlaca());
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            log.error("[TicketParqueoService] Error generando ticket PDF", e);
            throw new RuntimeException("No se pudo generar el ticket de parqueo", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addLabelValue(Document doc, String label, String value) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(padRight(label, 12), FONT_BODY));
        p.add(new Chunk(value, FONT_HEADER));
        p.setSpacingAfter(2f);
        doc.add(p);
    }

    private Paragraph separador(char car) {
        String linea = String.valueOf(car).repeat(36);
        Paragraph p = new Paragraph(linea, FONT_SMALL);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(2f);
        return p;
    }

    private Paragraph espaciado(float espacio) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(espacio);
        return p;
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private String formatearValor(PrecioSalidaResponse precio) {
        if (precio.getTotalPagado() != null) {
            return "$ " + precio.getTotalPagado().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        if (precio.getTotal() != null) {
            return "$ " + precio.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return "$ 0.00";
    }

    private String valor(String value) {
        return value != null && !value.isBlank() ? value : "N/A";
    }
}
