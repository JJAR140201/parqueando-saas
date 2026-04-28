package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SuscripcionMensualJpaEntity;

public interface SuscripcionMensualJpaRepository extends JpaRepository<SuscripcionMensualJpaEntity, Long> {
    List<SuscripcionMensualJpaEntity> findByEmpresaIdAndSedeId(Long empresaId, Long sedeId);

    Optional<SuscripcionMensualJpaEntity> findFirstByPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
        String placa,
        Long sedeId,
        Long empresaId,
        LocalDate fecha1,
        LocalDate fecha2
    );

    boolean existsByPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        String placa,
        Long sedeId,
        Long empresaId,
        LocalDate fechaFin,
        LocalDate fechaInicio
    );

    boolean existsByIdNotAndPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
        Long id,
        String placa,
        Long sedeId,
        Long empresaId,
        LocalDate fechaFin,
        LocalDate fechaInicio
    );
}
