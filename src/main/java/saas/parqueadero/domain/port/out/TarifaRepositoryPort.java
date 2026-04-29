package saas.parqueadero.domain.port.out;

import java.util.Optional;
import java.util.List;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.model.TipoVehiculo;

public interface TarifaRepositoryPort {
    Optional<Tarifa> findBySedeIdAndEmpresaIdAndTipoVehiculo(Long sedeId, Long empresaId, TipoVehiculo tipoVehiculo);

    List<Tarifa> findBySedeIdAndEmpresaId(Long sedeId, Long empresaId);

    Tarifa save(Tarifa tarifa);

    void deleteByEmpresaId(Long empresaId);
}
