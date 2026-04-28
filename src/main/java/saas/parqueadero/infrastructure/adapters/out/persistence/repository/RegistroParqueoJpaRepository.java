package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RegistroParqueoJpaEntity;

public interface RegistroParqueoJpaRepository extends JpaRepository<RegistroParqueoJpaEntity, Long>, JpaSpecificationExecutor<RegistroParqueoJpaEntity> {
    Optional<RegistroParqueoJpaEntity> findByPlacaAndSedeIdAndEmpresaIdAndEstado(
        String placa,
        Long sedeId,
        Long empresaId,
        EstadoRegistroParqueo estado
    );

    void deleteByEmpresaId(Long empresaId);
}
