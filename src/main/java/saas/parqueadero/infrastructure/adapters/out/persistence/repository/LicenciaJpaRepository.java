package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.LicenciaJpaEntity;

public interface LicenciaJpaRepository extends JpaRepository<LicenciaJpaEntity, Long> {
    Optional<LicenciaJpaEntity> findByCodigo(String codigo);

    List<LicenciaJpaEntity> findAllByEmpresaId(Long empresaId);

    List<LicenciaJpaEntity> findAllByOrderByFechaEmisionDesc();
}
