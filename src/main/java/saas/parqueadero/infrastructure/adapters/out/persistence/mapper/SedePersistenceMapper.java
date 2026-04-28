package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SedeJpaEntity;

@Mapper(componentModel = "spring")
public interface SedePersistenceMapper {
    Sede toDomain(SedeJpaEntity entity);

    SedeJpaEntity toEntity(Sede domain);
}
