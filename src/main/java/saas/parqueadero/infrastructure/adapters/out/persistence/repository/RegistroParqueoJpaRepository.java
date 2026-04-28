package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RegistroParqueoJpaEntity;

public interface RegistroParqueoJpaRepository extends JpaRepository<RegistroParqueoJpaEntity, Long> {
    Optional<RegistroParqueoJpaEntity> findByPlacaAndSedeIdAndEmpresaIdAndEstado(
        String placa,
        Long sedeId,
        Long empresaId,
        EstadoRegistroParqueo estado
    );

    void deleteByEmpresaId(Long empresaId);

    @Query("SELECT r FROM RegistroParqueoJpaEntity r WHERE (:empresaId IS NULL OR r.empresaId = :empresaId) AND (:sedeId IS NULL OR r.sedeId = :sedeId) AND (:estado IS NULL OR r.estado = :estado) AND (:desde IS NULL OR r.fechaEntrada >= :desde) AND (:hasta IS NULL OR r.fechaEntrada <= :hasta) ORDER BY r.fechaEntrada DESC")
    List<RegistroParqueoJpaEntity> findReporte(
        @Param("empresaId") Long empresaId,
        @Param("sedeId") Long sedeId,
        @Param("estado") EstadoRegistroParqueo estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );
}
