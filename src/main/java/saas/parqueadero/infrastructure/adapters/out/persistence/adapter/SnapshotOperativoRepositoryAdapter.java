package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.domain.model.SnapshotOperativo;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.SnapshotOperativoPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.SnapshotOperativoJpaRepository;

@Component
@RequiredArgsConstructor
public class SnapshotOperativoRepositoryAdapter implements SnapshotOperativoRepositoryPort {

    private final SnapshotOperativoJpaRepository repository;
    private final SnapshotOperativoPersistenceMapper mapper;

    @Override
    public SnapshotOperativo save(SnapshotOperativo snapshot) {
        return mapper.toDomain(repository.save(mapper.toEntity(snapshot)));
    }

    @Override
    public Optional<SnapshotOperativo> findUltimoByTenantId(String tenantId) {
        return repository.findFirstByTenantIdOrderByRecibidoEnDesc(tenantId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public int deleteRecibidosAntesDe(Instant limite) {
        return repository.deleteByRecibidoEnBefore(limite);
    }
}
