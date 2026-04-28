package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.domain.model.TipoVehiculo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.TarifaJpaEntity;

public interface TarifaJpaRepository extends JpaRepository<TarifaJpaEntity, Long> {
    Optional<TarifaJpaEntity> findBySedeIdAndEmpresaIdAndTipoVehiculo(Long sedeId, Long empresaId, TipoVehiculo tipoVehiculo);

    void deleteByEmpresaId(Long empresaId);
}
