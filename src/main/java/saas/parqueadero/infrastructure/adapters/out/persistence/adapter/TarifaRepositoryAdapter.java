package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.domain.model.TipoVehiculo;
import saas.parqueadero.domain.port.out.TarifaRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.TarifaPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.TarifaJpaRepository;

@Component
@RequiredArgsConstructor
public class TarifaRepositoryAdapter implements TarifaRepositoryPort {

    private final TarifaJpaRepository tarifaJpaRepository;
    private final TarifaPersistenceMapper mapper;

    @Override
    public Optional<Tarifa> findBySedeIdAndEmpresaIdAndTipoVehiculo(Long sedeId, Long empresaId, TipoVehiculo tipoVehiculo) {
        return tarifaJpaRepository.findBySedeIdAndEmpresaIdAndTipoVehiculo(sedeId, empresaId, tipoVehiculo)
            .map(mapper::toDomain);
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        tarifaJpaRepository.deleteByEmpresaId(empresaId);
    }
}
