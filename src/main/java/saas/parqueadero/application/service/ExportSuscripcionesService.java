package saas.parqueadero.application.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;

import saas.parqueadero.domain.model.SuscripcionMensual;

@Service
public class ExportSuscripcionesService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportToExcel(List<SuscripcionMensual> suscripciones) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Mensualidades");
            
            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Placa", "Tipo Vehículo", "Valor Mensual", "Fecha Inicio", 
                               "Fecha Fin", "Activa", "Empresa ID", "Sede ID"};
            
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // Llenar datos
            int rowNum = 1;
            for (SuscripcionMensual suscripcion : suscripciones) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(suscripcion.getId());
                row.createCell(1).setCellValue(suscripcion.getPlaca());
                row.createCell(2).setCellValue(suscripcion.getTipoVehiculo().name());
                row.createCell(3).setCellValue(suscripcion.getValorMensual().doubleValue());
                row.createCell(4).setCellValue(suscripcion.getFechaInicio().format(DATE_FORMATTER));
                row.createCell(5).setCellValue(suscripcion.getFechaFin().format(DATE_FORMATTER));
                row.createCell(6).setCellValue(suscripcion.getActiva() ? "Sí" : "No");
                row.createCell(7).setCellValue(suscripcion.getEmpresaId());
                row.createCell(8).setCellValue(suscripcion.getSedeId());
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportToPdf(List<SuscripcionMensual> suscripciones) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        Document document = new Document(PageSize.A4.rotate());
        com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
        
        document.open();
        
        // Título
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("Reporte de Mensualidades", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));
        
        // Tabla
        Table table = new Table(9);
        table.setWidth(100);
        table.setPadding(5);
        table.setSpacing(5);
        
        // Encabezados
        String[] headers = {"ID", "Placa", "Tipo Vehículo", "Valor Mensual", "Fecha Inicio", 
                           "Fecha Fin", "Activa", "Empresa", "Sede"};
        for (String header : headers) {
            com.lowagie.text.Cell cell = new com.lowagie.text.Cell(header);
            cell.setHeader(true);
            table.addCell(cell);
        }
        
        // Datos
        for (SuscripcionMensual suscripcion : suscripciones) {
            table.addCell(String.valueOf(suscripcion.getId()));
            table.addCell(suscripcion.getPlaca());
            table.addCell(suscripcion.getTipoVehiculo().name());
            table.addCell(suscripcion.getValorMensual().toString());
            table.addCell(suscripcion.getFechaInicio().format(DATE_FORMATTER));
            table.addCell(suscripcion.getFechaFin().format(DATE_FORMATTER));
            table.addCell(suscripcion.getActiva() ? "Sí" : "No");
            table.addCell(String.valueOf(suscripcion.getEmpresaId()));
            table.addCell(String.valueOf(suscripcion.getSedeId()));
        }
        
        document.add(table);
        document.close();
        
        return baos.toByteArray();
    }
}
