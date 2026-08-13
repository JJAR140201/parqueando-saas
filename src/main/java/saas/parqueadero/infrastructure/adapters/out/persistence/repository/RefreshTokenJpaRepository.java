package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RefreshTokenJpaEntity;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenJpaEntity t set t.revoked = true where t.usuarioId = :usuarioId and t.revoked = false")
    void revokeAllActiveByUsuarioId(@Param("usuarioId") Long usuarioId);
}
