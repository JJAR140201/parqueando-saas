package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SedeJpaEntity;

public interface SedeJpaRepository extends JpaRepository<SedeJpaEntity, Long> {
    List<SedeJpaEntity> findByEmpresaId(Long empresaId);

    Optional<SedeJpaEntity> findByIdAndEmpresaId(Long id, Long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SedeJpaEntity s where s.id = :id and s.empresaId = :empresaId")
    Optional<SedeJpaEntity> findByIdAndEmpresaIdForUpdate(@Param("id") Long id, @Param("empresaId") Long empresaId);

    void deleteByIdAndEmpresaId(Long id, Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
