package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.EmpresaPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.EmpresaJpaRepository;

@Component
@RequiredArgsConstructor
public class EmpresaRepositoryAdapter implements EmpresaRepositoryPort {

    private final EmpresaJpaRepository empresaJpaRepository;
    private final EmpresaPersistenceMapper mapper;

    @Override
    public List<Empresa> findAll() {
        return empresaJpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Empresa> findByNit(String nit) {
        return empresaJpaRepository.findByNit(nit)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Empresa> findById(Long id) {
        return empresaJpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Empresa save(Empresa empresa) {
        return mapper.toDomain(empresaJpaRepository.save(mapper.toEntity(empresa)));
    }

    @Override
    public void deleteById(Long id) {
        empresaJpaRepository.deleteById(id);
    }
}
