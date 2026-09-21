package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SnapshotOperativoJpaEntity;

public interface SnapshotOperativoJpaRepository extends JpaRepository<SnapshotOperativoJpaEntity, Long> {

    Optional<SnapshotOperativoJpaEntity> findFirstByTenantIdOrderByRecibidoEnDesc(String tenantId);

    @Modifying
    @Query("DELETE FROM SnapshotOperativoJpaEntity s WHERE s.recibidoEn < :limite")
    int deleteByRecibidoEnBefore(@Param("limite") Instant limite);
}
