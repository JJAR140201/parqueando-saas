package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Licencia;
import saas.parqueadero.domain.port.out.LicenciaRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.LicenciaPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.LicenciaJpaRepository;

@Component
@RequiredArgsConstructor
public class LicenciaRepositoryAdapter implements LicenciaRepositoryPort {

    private final LicenciaJpaRepository licenciaJpaRepository;
    private final LicenciaPersistenceMapper mapper;

    @Override
    public Optional<Licencia> findById(Long id) {
        return licenciaJpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Licencia> findByCodigo(String codigoNormalizado) {
        return licenciaJpaRepository.findByCodigo(codigoNormalizado)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Licencia> findByEmpresaId(Long empresaId) {
        return licenciaJpaRepository.findByEmpresaId(empresaId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Licencia> findAll() {
        return licenciaJpaRepository.findAllByOrderByFechaEmisionDesc().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Licencia save(Licencia licencia) {
        return mapper.toDomain(licenciaJpaRepository.save(mapper.toEntity(licencia)));
    }
}
