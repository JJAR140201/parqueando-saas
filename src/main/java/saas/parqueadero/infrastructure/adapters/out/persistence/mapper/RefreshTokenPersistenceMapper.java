package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.RefreshToken;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.RefreshTokenJpaEntity;

@Mapper(componentModel = "spring")
public interface RefreshTokenPersistenceMapper {
    RefreshToken toDomain(RefreshTokenJpaEntity entity);

    RefreshTokenJpaEntity toEntity(RefreshToken domain);
}
