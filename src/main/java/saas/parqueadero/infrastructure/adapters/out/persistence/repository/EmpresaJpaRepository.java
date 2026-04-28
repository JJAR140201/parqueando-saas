package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.EmpresaJpaEntity;

public interface EmpresaJpaRepository extends JpaRepository<EmpresaJpaEntity, Long> {
    Optional<EmpresaJpaEntity> findByNit(String nit);
}
