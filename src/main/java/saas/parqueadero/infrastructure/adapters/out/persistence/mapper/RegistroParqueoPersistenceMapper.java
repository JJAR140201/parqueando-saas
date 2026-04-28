package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.RegistroParqueo;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RegistroParqueoJpaEntity;

@Mapper(componentModel = "spring")
public interface RegistroParqueoPersistenceMapper {
    RegistroParqueo toDomain(RegistroParqueoJpaEntity entity);

    RegistroParqueoJpaEntity toEntity(RegistroParqueo domain);
}
