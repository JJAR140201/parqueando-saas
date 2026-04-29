package saas.parqueadero.domain.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.SuscripcionMensual;

public interface SuscripcionMensualRepositoryPort {
    SuscripcionMensual save(SuscripcionMensual suscripcionMensual);

    Optional<SuscripcionMensual> findById(Long id);

    List<SuscripcionMensual> findByEmpresaIdAndSedeId(Long empresaId, Long sedeId);

    Optional<SuscripcionMensual> findVigenteByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId, LocalDate fecha);

    boolean existsActivaOverlap(String placa, Long sedeId, Long empresaId, LocalDate fechaInicio, LocalDate fechaFin);

    boolean existsActivaOverlapExcludingId(Long id, String placa, Long sedeId, Long empresaId, LocalDate fechaInicio, LocalDate fechaFin);

    List<SuscripcionMensual> findAll();
}
