package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.ClienteInstalacionJpaEntity;

public interface ClienteInstalacionJpaRepository extends JpaRepository<ClienteInstalacionJpaEntity, Long> {

    Optional<ClienteInstalacionJpaEntity> findByTenantId(String tenantId);

    boolean existsByEmpresaId(Long empresaId);
}
