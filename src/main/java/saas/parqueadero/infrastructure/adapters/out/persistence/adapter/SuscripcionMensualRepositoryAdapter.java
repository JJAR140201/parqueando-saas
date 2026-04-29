package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.SuscripcionMensualPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.SuscripcionMensualJpaRepository;

@Component
@RequiredArgsConstructor
public class SuscripcionMensualRepositoryAdapter implements SuscripcionMensualRepositoryPort {

    private final SuscripcionMensualJpaRepository suscripcionMensualJpaRepository;
    private final SuscripcionMensualPersistenceMapper mapper;

    @Override
    public SuscripcionMensual save(SuscripcionMensual suscripcionMensual) {
        return mapper.toDomain(suscripcionMensualJpaRepository.save(mapper.toEntity(suscripcionMensual)));
    }

    @Override
    public Optional<SuscripcionMensual> findById(Long id) {
        return suscripcionMensualJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SuscripcionMensual> findByEmpresaIdAndSedeId(Long empresaId, Long sedeId) {
        return suscripcionMensualJpaRepository.findByEmpresaIdAndSedeId(empresaId, sedeId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<SuscripcionMensual> findVigenteByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId, LocalDate fecha) {
        return suscripcionMensualJpaRepository
            .findFirstByPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
                placa.trim().toUpperCase(),
                sedeId,
                empresaId,
                fecha,
                fecha
            )
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsActivaOverlap(String placa, Long sedeId, Long empresaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return suscripcionMensualJpaRepository.existsByPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            placa.trim().toUpperCase(),
            sedeId,
            empresaId,
            fechaFin,
            fechaInicio
        );
    }

    @Override
    public boolean existsActivaOverlapExcludingId(Long id, String placa, Long sedeId, Long empresaId, LocalDate fechaInicio, LocalDate fechaFin) {
        return suscripcionMensualJpaRepository.existsByIdNotAndPlacaAndSedeIdAndEmpresaIdAndActivaTrueAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
            id,
            placa.trim().toUpperCase(),
            sedeId,
            empresaId,
            fechaFin,
            fechaInicio
        );
    }

    @Override
    public List<SuscripcionMensual> findAll() {
        return suscripcionMensualJpaRepository.findAll()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
