package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.SuscripcionMensualJpaEntity;

@Mapper(componentModel = "spring")
public interface SuscripcionMensualPersistenceMapper {
    SuscripcionMensual toDomain(SuscripcionMensualJpaEntity entity);

    SuscripcionMensualJpaEntity toEntity(SuscripcionMensual domain);
}
