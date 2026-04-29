package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import java.util.List;
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
    public List<Tarifa> findBySedeIdAndEmpresaId(Long sedeId, Long empresaId) {
        return tarifaJpaRepository.findBySedeIdAndEmpresaId(sedeId, empresaId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Tarifa save(Tarifa tarifa) {
        return mapper.toDomain(tarifaJpaRepository.save(mapper.toEntity(tarifa)));
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        tarifaJpaRepository.deleteByEmpresaId(empresaId);
    }
}
