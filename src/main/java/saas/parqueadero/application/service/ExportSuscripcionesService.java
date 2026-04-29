package saas.parqueadero.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;

@Service
@RequiredArgsConstructor
public class ExportSuscripcionesService {

    private final SedeRepositoryPort sedeRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] HEADERS = {
        "ID", "Placa", "Tipo Vehiculo", "Valor Mensual", "Fecha Inicio",
        "Fecha Fin", "Activa", "Sede", "Usuario"
    };

    public byte[] exportToExcel(List<SuscripcionMensual> suscripciones) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Mensualidades");
            Map<String, String> sedeCache = new HashMap<>();
            String usuarioNombre = resolveUsuarioNombre();

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle altStyle = workbook.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowNum = 1;
            for (SuscripcionMensual suscripcion : suscripciones) {
                Row row = sheet.createRow(rowNum);
                CellStyle style = (rowNum % 2 == 0) ? altStyle : null;

                setCell(row, 0, suscripcion.getId() != null ? suscripcion.getId().toString() : "", style);
                setCell(row, 1, suscripcion.getPlaca(), style);
                setCell(row, 2, suscripcion.getTipoVehiculo() != null ? suscripcion.getTipoVehiculo().name() : "", style);
                setCell(row, 3, suscripcion.getValorMensual() != null ? "$" + suscripcion.getValorMensual().toPlainString() : "-", style);
                setCell(row, 4, suscripcion.getFechaInicio() != null ? suscripcion.getFechaInicio().format(DATE_FORMATTER) : "", style);
                setCell(row, 5, suscripcion.getFechaFin() != null ? suscripcion.getFechaFin().format(DATE_FORMATTER) : "", style);
                setCell(row, 6, Boolean.TRUE.equals(suscripcion.getActiva()) ? "Si" : "No", style);
                setCell(row, 7, resolveSedeNombre(suscripcion, sedeCache), style);
                setCell(row, 8, usuarioNombre, style);
                rowNum++;
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Row totalsRow = sheet.createRow(rowNum + 1);
            totalsRow.createCell(0).setCellValue("TOTAL REGISTROS:");
            totalsRow.createCell(1).setCellValue(suscripciones.size());
            BigDecimal totalMensual = suscripciones.stream()
                .filter(s -> s.getValorMensual() != null)
                .map(SuscripcionMensual::getValorMensual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalsRow.createCell(3).setCellValue("TOTAL VALOR MENSUAL:");
            totalsRow.createCell(4).setCellValue("$" + totalMensual.toPlainString());

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportToPdf(List<SuscripcionMensual> suscripciones) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();
        Map<String, String> sedeCache = new HashMap<>();
        String usuarioNombre = resolveUsuarioNombre();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        document.add(new Paragraph("Reporte de Mensualidades", titleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(0, 51, 102));
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }

        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        boolean alt = false;
        for (SuscripcionMensual suscripcion : suscripciones) {
            Color bg = alt ? new Color(220, 230, 241) : Color.WHITE;

            addPdfCell(table, dataFont, bg, suscripcion.getId() != null ? suscripcion.getId().toString() : "");
            addPdfCell(table, dataFont, bg, suscripcion.getPlaca() != null ? suscripcion.getPlaca() : "");
            addPdfCell(table, dataFont, bg, suscripcion.getTipoVehiculo() != null ? suscripcion.getTipoVehiculo().name() : "");
            addPdfCell(table, dataFont, bg, suscripcion.getValorMensual() != null ? "$" + suscripcion.getValorMensual().toPlainString() : "-");
            addPdfCell(table, dataFont, bg, suscripcion.getFechaInicio() != null ? suscripcion.getFechaInicio().format(DATE_FORMATTER) : "");
            addPdfCell(table, dataFont, bg, suscripcion.getFechaFin() != null ? suscripcion.getFechaFin().format(DATE_FORMATTER) : "");
            addPdfCell(table, dataFont, bg, Boolean.TRUE.equals(suscripcion.getActiva()) ? "Si" : "No");
            addPdfCell(table, dataFont, bg, resolveSedeNombre(suscripcion, sedeCache));
            addPdfCell(table, dataFont, bg, usuarioNombre);
            alt = !alt;
        }

        document.add(table);

        document.add(new Paragraph(" "));
        BigDecimal totalMensual = suscripciones.stream()
            .filter(s -> s.getValorMensual() != null)
            .map(SuscripcionMensual::getValorMensual)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        document.add(new Paragraph("Total registros: " + suscripciones.size(), summaryFont));
        document.add(new Paragraph("Total valor mensual: $" + totalMensual.toPlainString(), summaryFont));

        document.close();
        return out.toByteArray();
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void addPdfCell(PdfPTable table, Font font, Color bg, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(3);
        table.addCell(cell);
    }

    private String resolveSedeNombre(SuscripcionMensual suscripcion, Map<String, String> sedeCache) {
        if (suscripcion.getEmpresaId() == null || suscripcion.getSedeId() == null) {
            return "-";
        }
        String key = suscripcion.getEmpresaId() + ":" + suscripcion.getSedeId();
        return sedeCache.computeIfAbsent(key, k -> sedeRepositoryPort
            .findByIdAndEmpresaId(suscripcion.getSedeId(), suscripcion.getEmpresaId())
            .map(Sede::getNombre)
            .orElse("-"));
    }

    private String resolveUsuarioNombre() {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        if (currentUser == null) {
            return "-";
        }
        if (currentUser.getNombre() != null && !currentUser.getNombre().isBlank()) {
            return currentUser.getNombre();
        }
        if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            return currentUser.getUsername();
        }
        return "-";
    }
}
