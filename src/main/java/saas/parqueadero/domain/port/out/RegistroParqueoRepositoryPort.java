package saas.parqueadero.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;

public interface RegistroParqueoRepositoryPort {
    RegistroParqueo save(RegistroParqueo registroParqueo);

    Optional<RegistroParqueo> findActivoByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId);

    List<RegistroParqueo> findReporte(Long empresaId, Long sedeId, EstadoRegistroParqueo estado, LocalDateTime desde, LocalDateTime hasta);

    void deleteByEmpresaId(Long empresaId);
}
