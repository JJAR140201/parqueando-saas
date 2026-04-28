package saas.parqueadero.domain.port.out;

import java.util.Optional;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.model.TipoVehiculo;

public interface TarifaRepositoryPort {
    Optional<Tarifa> findBySedeIdAndEmpresaIdAndTipoVehiculo(Long sedeId, Long empresaId, TipoVehiculo tipoVehiculo);

    void deleteByEmpresaId(Long empresaId);
}
