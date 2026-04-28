package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SedeJpaEntity;

public interface SedeJpaRepository extends JpaRepository<SedeJpaEntity, Long> {
    List<SedeJpaEntity> findByEmpresaId(Long empresaId);

    Optional<SedeJpaEntity> findByIdAndEmpresaId(Long id, Long empresaId);

    void deleteByIdAndEmpresaId(Long id, Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
