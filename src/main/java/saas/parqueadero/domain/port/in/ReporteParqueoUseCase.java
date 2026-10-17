package saas.parqueadero.domain.port.in;

import java.time.LocalDate;
import java.util.List;
import saas.parqueadero.application.dto.ReportePaginaResponse;
import saas.parqueadero.application.dto.ReporteRegistroResponse;
import saas.parqueadero.application.dto.ResumenDiaResponse;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;

public interface ReporteParqueoUseCase {

    /**
     * Una pagina del reporte segun el alcance permitido por el rol.
     *
     * @param empresaId Filtro opcional. Solo aplica para SUPER_ADMIN.
     * @param sedeId    Filtro opcional. Solo aplica para SUPER_ADMIN.
     * @param estado    Filtra por estado (ACTIVO/FINALIZADO). Null = todos.
     * @param desde     Fecha de inicio del rango (fechaEntrada >= desde). Null = sin limite.
     * @param hasta     Fecha de fin del rango (fechaEntrada <= hasta). Null = sin limite.
     * @param pagina    Numero de pagina, desde 0.
     * @param tamano    Elementos por pagina (1 a {@link #TAMANO_MAXIMO_PAGINA}).
     */
    ReportePaginaResponse getReportePagina(Long empresaId, Long sedeId, EstadoRegistroParqueo estado,
        LocalDate desde, LocalDate hasta, int pagina, int tamano);

    /**
     * Todos los registros del filtro para exportar a Excel/PDF. Falla si superan
     * {@link #MAX_FILAS_EXPORTACION}: hay que acotar el rango de fechas.
     */
    List<ReporteRegistroResponse> getReporteParaExportar(Long empresaId, Long sedeId, EstadoRegistroParqueo estado,
        LocalDate desde, LocalDate hasta);

    int TAMANO_MAXIMO_PAGINA = 5000;
    int MAX_FILAS_EXPORTACION = 50_000;

    /**
     * Conteo de vehiculos dentro ahora mismo, y entradas/salidas de hoy, por tipo de vehiculo.
     * Visible para los 3 roles; ADMIN/OPERARIO quedan restringidos a su propia empresa/sede.
     *
     * @param empresaId Filtro opcional. Solo aplica para SUPER_ADMIN.
     * @param sedeId    Filtro opcional. Solo aplica para SUPER_ADMIN.
     */
    ResumenDiaResponse getResumenDia(Long empresaId, Long sedeId);
}
