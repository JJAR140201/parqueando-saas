package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Licencia;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.LicenciaJpaEntity;

@Mapper(componentModel = "spring")
public interface LicenciaPersistenceMapper {
    Licencia toDomain(LicenciaJpaEntity entity);

    LicenciaJpaEntity toEntity(Licencia domain);
}
