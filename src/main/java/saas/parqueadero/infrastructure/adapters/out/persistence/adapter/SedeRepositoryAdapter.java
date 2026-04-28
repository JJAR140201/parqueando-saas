package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.SedePersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.SedeJpaRepository;

@Component
@RequiredArgsConstructor
public class SedeRepositoryAdapter implements SedeRepositoryPort {

    private final SedeJpaRepository sedeJpaRepository;
    private final SedePersistenceMapper mapper;

    @Override
    public List<Sede> findByEmpresaId(Long empresaId) {
        return sedeJpaRepository.findByEmpresaId(empresaId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Sede> findByIdAndEmpresaId(Long id, Long empresaId) {
        return sedeJpaRepository.findByIdAndEmpresaId(id, empresaId)
            .map(mapper::toDomain);
    }

    @Override
    public Sede save(Sede sede) {
        return mapper.toDomain(sedeJpaRepository.save(mapper.toEntity(sede)));
    }

    @Override
    public void deleteByIdAndEmpresaId(Long id, Long empresaId) {
        sedeJpaRepository.deleteByIdAndEmpresaId(id, empresaId);
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        sedeJpaRepository.deleteByEmpresaId(empresaId);
    }
}
