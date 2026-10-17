package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.Pagina;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.domain.port.out.RegistroParqueoRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.RegistroParqueoPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RegistroParqueoJpaEntity;
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
    public Optional<RegistroParqueo> findUltimoFinalizadoByPlacaAndSedeIdAndEmpresaId(String placa, Long sedeId, Long empresaId) {
        return registroParqueoJpaRepository
            .findTopByPlacaAndSedeIdAndEmpresaIdAndEstadoOrderByFechaSalidaDesc(
                placa.trim().toUpperCase(),
                sedeId,
                empresaId,
                EstadoRegistroParqueo.FINALIZADO
            )
            .map(mapper::toDomain);
    }

    @Override
    public Pagina<RegistroParqueo> findReportePagina(Long empresaId, Long sedeId, EstadoRegistroParqueo estado,
        LocalDateTime desde, LocalDateTime hasta, int pagina, int tamano) {
        Specification<RegistroParqueoJpaEntity> specification = Specification.where(null);

        if (empresaId != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("empresaId"), empresaId));
        }

        if (sedeId != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("sedeId"), sedeId));
        }

        if (estado != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("estado"), estado));
        }

        if (desde != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("fechaEntrada"), desde));
        }

        if (hasta != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("fechaEntrada"), hasta));
        }

        Page<RegistroParqueoJpaEntity> resultado = registroParqueoJpaRepository.findAll(specification,
            PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaEntrada").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new Pagina<>(
            resultado.getContent().stream().map(mapper::toDomain).collect(Collectors.toList()),
            resultado.getTotalElements());
    }

    @Override
    public List<RegistroParqueo> findActividadDelDia(Long empresaId, Long sedeId, LocalDateTime inicioDia, LocalDateTime finDia) {
        Specification<RegistroParqueoJpaEntity> specification = Specification.where(null);

        if (empresaId != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("empresaId"), empresaId));
        }

        if (sedeId != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("sedeId"), sedeId));
        }

        Specification<RegistroParqueoJpaEntity> actividadDelDia = (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(root.get("estado"), EstadoRegistroParqueo.ACTIVO),
            criteriaBuilder.between(root.get("fechaEntrada"), inicioDia, finDia),
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("estado"), EstadoRegistroParqueo.FINALIZADO),
                criteriaBuilder.between(root.get("fechaSalida"), inicioDia, finDia)
            )
        );

        return registroParqueoJpaRepository.findAll(specification.and(actividadDelDia)).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        registroParqueoJpaRepository.deleteByEmpresaId(empresaId);
    }
}
