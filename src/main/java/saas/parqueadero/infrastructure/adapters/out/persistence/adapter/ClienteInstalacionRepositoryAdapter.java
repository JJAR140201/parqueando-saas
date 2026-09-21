package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.port.out.ClienteInstalacionRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.ClienteInstalacionPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.ClienteInstalacionJpaRepository;

@Component
@RequiredArgsConstructor
public class ClienteInstalacionRepositoryAdapter implements ClienteInstalacionRepositoryPort {

    private final ClienteInstalacionJpaRepository repository;
    private final ClienteInstalacionPersistenceMapper mapper;

    @Override
    public List<ClienteInstalacion> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClienteInstalacion> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmpresaId(Long empresaId) {
        return empresaId != null && repository.existsByEmpresaId(empresaId);
    }

    @Override
    public ClienteInstalacion save(ClienteInstalacion cliente) {
        return mapper.toDomain(repository.save(mapper.toEntity(cliente)));
    }
}
