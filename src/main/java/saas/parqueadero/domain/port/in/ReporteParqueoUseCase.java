package saas.parqueadero.domain.port.in;

import java.time.LocalDate;
import java.util.List;
import saas.parqueadero.application.dto.ReporteRegistroResponse;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;

public interface ReporteParqueoUseCase {

    /**
     * Retorna los registros de parqueo según el alcance permitido por el rol.
     *
     * @param empresaId Filtro opcional. Solo aplica para SUPER_ADMIN.
     * @param sedeId    Filtro opcional. Solo aplica para SUPER_ADMIN.
     * @param estado    Filtra por estado (ACTIVO/FINALIZADO). Null = todos.
     * @param desde     Fecha de inicio del rango (fechaEntrada >= desde). Null = sin limite.
     * @param hasta     Fecha de fin del rango (fechaEntrada <= hasta). Null = sin limite.
     */
    List<ReporteRegistroResponse> getReporte(Long empresaId, Long sedeId, EstadoRegistroParqueo estado, LocalDate desde, LocalDate hasta);
}
