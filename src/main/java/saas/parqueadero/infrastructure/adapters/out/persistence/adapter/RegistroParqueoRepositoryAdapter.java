package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.RegistroParqueoPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.RegistroParqueoJpaRepository;

@Component
@RequiredArgsConstructor
public class RegistroParqueoRepositoryAdapter implements RegistroParqueoRepositoryPort {

    private final RegistroParqueoJpaRepository registroParqueoJpaRepository;
    private final RegistroParqueoPersistenceMapper mapper;

    @Override
    public RegistroParqueo save(RegistroParqueo registroParqueo) {
        return mapper.toDomain(registroParqueoJpaRepository.save(mapper.toEntity(registroParqueo)));
    }

    @Override
    public Optional<RegistroParqueo> findActivoByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId) {
        return registroParqueoJpaRepository.findByPlacaAndSedeIdAndEmpresaIdAndEstado(
                placa.trim().toUpperCase(),
                sedeId,
                empresaId,
                EstadoRegistroParqueo.ACTIVO
            )
            .map(mapper::toDomain);
    }

    @Override
    public List<RegistroParqueo> findReporte(Long empresaId, Long sedeId, EstadoRegistroParqueo estado, LocalDateTime desde, LocalDateTime hasta) {
        return registroParqueoJpaRepository.findReporte(empresaId, sedeId, estado, desde, hasta).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        registroParqueoJpaRepository.deleteByEmpresaId(empresaId);
    }
}
