package saas.parqueadero.infrastructure.adapters.in.rest.controller;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.ReporteRegistroResponse;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.port.in.ReporteParqueoUseCase;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteParqueoController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] HEADERS = {
        "ID", "Placa", "Tipo Vehiculo", "Fecha Entrada", "Fecha Salida",
        "Minutos Estadia", "Total Pagado", "Estado", "Sede ID", "Usuario ID"
    };

    private final ReporteParqueoUseCase reporteParqueoUseCase;

    @GetMapping("/parqueo")
    public ResponseEntity<List<ReporteRegistroResponse>> getReporteJson(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) EstadoRegistroParqueo estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(reporteParqueoUseCase.getReporte(empresaId, sedeId, estado, desde, hasta));
    }

    @GetMapping("/parqueo/excel")
    public ResponseEntity<byte[]> getReporteExcel(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) EstadoRegistroParqueo estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) throws IOException {
        List<ReporteRegistroResponse> registros = reporteParqueoUseCase.getReporte(empresaId, sedeId, estado, desde, hasta);
        byte[] bytes = buildExcel(registros);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("reporte_parqueo.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/parqueo/pdf")
    public ResponseEntity<byte[]> getReportePdf(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) EstadoRegistroParqueo estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        List<ReporteRegistroResponse> registros = reporteParqueoUseCase.getReporte(empresaId, sedeId, estado, desde, hasta);
        byte[] bytes = buildPdf(registros);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("reporte_parqueo.pdf").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private byte[] buildExcel(List<ReporteRegistroResponse> registros) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Reporte Parqueo");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowNum = 1;
            for (ReporteRegistroResponse r : registros) {
                Row row = sheet.createRow(rowNum);
                CellStyle style = (rowNum % 2 == 0) ? altStyle : null;

                setCell(row, 0, r.getId() != null ? r.getId().toString() : "", style);
                setCell(row, 1, r.getPlaca(), style);
                setCell(row, 2, r.getTipoVehiculo() != null ? r.getTipoVehiculo().name() : "", style);
                setCell(row, 3, r.getFechaEntrada() != null ? r.getFechaEntrada().format(DT_FMT) : "", style);
                setCell(row, 4, r.getFechaSalida() != null ? r.getFechaSalida().format(DT_FMT) : "En curso", style);
                setCell(row, 5, r.getMinutosEstadia() != null ? r.getMinutosEstadia().toString() : "-", style);
                setCell(row, 6, r.getTotalPagado() != null ? "$" + r.getTotalPagado().toPlainString() : "-", style);
                setCell(row, 7, r.getEstado() != null ? r.getEstado().name() : "", style);
                setCell(row, 8, r.getSedeId() != null ? r.getSedeId().toString() : "", style);
                setCell(row, 9, r.getUsuarioId() != null ? r.getUsuarioId().toString() : "", style);
                rowNum++;
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Row totalsRow = sheet.createRow(rowNum + 1);
            totalsRow.createCell(0).setCellValue("TOTAL REGISTROS:");
            totalsRow.createCell(1).setCellValue(registros.size());
            BigDecimal totalPagado = registros.stream()
                .filter(r -> r.getTotalPagado() != null)
                .map(ReporteRegistroResponse::getTotalPagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalsRow.createCell(5).setCellValue("TOTAL COBRADO:");
            totalsRow.createCell(6).setCellValue("$" + totalPagado.toPlainString());

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private byte[] buildPdf(List<ReporteRegistroResponse> registros) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
        doc.add(new Paragraph("Reporte de Parqueo", titleFont));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);

        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new Color(0, 51, 102));
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            cell.setPadding(4);
            table.addCell(cell);
        }

        Font dFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        boolean alt = false;
        for (ReporteRegistroResponse r : registros) {
            Color bg = alt ? new Color(220, 230, 241) : Color.WHITE;

            addPdfCell(table, dFont, bg, r.getId() != null ? r.getId().toString() : "");
            addPdfCell(table, dFont, bg, r.getPlaca());
            addPdfCell(table, dFont, bg, r.getTipoVehiculo() != null ? r.getTipoVehiculo().name() : "");
            addPdfCell(table, dFont, bg, r.getFechaEntrada() != null ? r.getFechaEntrada().format(DT_FMT) : "");
            addPdfCell(table, dFont, bg, r.getFechaSalida() != null ? r.getFechaSalida().format(DT_FMT) : "En curso");
            addPdfCell(table, dFont, bg, r.getMinutosEstadia() != null ? r.getMinutosEstadia().toString() : "-");
            addPdfCell(table, dFont, bg, r.getTotalPagado() != null ? "$" + r.getTotalPagado().toPlainString() : "-");
            addPdfCell(table, dFont, bg, r.getEstado() != null ? r.getEstado().name() : "");
            addPdfCell(table, dFont, bg, r.getSedeId() != null ? r.getSedeId().toString() : "");
            addPdfCell(table, dFont, bg, r.getUsuarioId() != null ? r.getUsuarioId().toString() : "");
            alt = !alt;
        }

        doc.add(table);

        doc.add(new Paragraph(" "));
        BigDecimal totalPagado = registros.stream()
            .filter(r -> r.getTotalPagado() != null)
            .map(ReporteRegistroResponse::getTotalPagado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Font sumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        doc.add(new Paragraph("Total registros: " + registros.size(), sumFont));
        doc.add(new Paragraph("Total cobrado: $" + totalPagado.toPlainString(), sumFont));

        doc.close();
        return out.toByteArray();
    }

    private void addPdfCell(PdfPTable table, Font font, Color bg, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(3);
        table.addCell(cell);
    }
}
