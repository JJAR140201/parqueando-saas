package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Tarifa;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.TarifaJpaEntity;

@Mapper(componentModel = "spring")
public interface TarifaPersistenceMapper {
    Tarifa toDomain(TarifaJpaEntity entity);

    TarifaJpaEntity toEntity(Tarifa domain);
}
