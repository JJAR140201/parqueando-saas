package saas.parqueadero.application.dto;

import java.util.List;

/** Pagina de un reporte de parqueo; el controlador expone el total en cabeceras. */
public record ReportePaginaResponse(List<ReporteRegistroResponse> items, long total) {
}
