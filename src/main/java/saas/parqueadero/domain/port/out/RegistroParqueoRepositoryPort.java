package saas.parqueadero.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.Pagina;
import saas.parqueadero.domain.model.RegistroParqueo;

public interface RegistroParqueoRepositoryPort {
    RegistroParqueo save(RegistroParqueo registroParqueo);

    Optional<RegistroParqueo> findActivoByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId);

    Optional<RegistroParqueo> findUltimoFinalizadoByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId);

    /** Pagina del reporte, ordenada por fecha de entrada descendente. {@code pagina} empieza en 0. */
    Pagina<RegistroParqueo> findReportePagina(Long empresaId, Long sedeId, EstadoRegistroParqueo estado,
        LocalDateTime desde, LocalDateTime hasta, int pagina, int tamano);

    /**
     * Registros relevantes para el resumen del dia: los que siguen ACTIVOS (sin importar cuando
     * entraron) mas los que tuvieron entrada o salida dentro de [inicioDia, finDia].
     */
    List<RegistroParqueo> findActividadDelDia(Long empresaId, Long sedeId, LocalDateTime inicioDia, LocalDateTime finDia);

    void deleteByEmpresaId(Long empresaId);
}
