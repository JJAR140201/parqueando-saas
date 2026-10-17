package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.WhatsappAccountJpaEntity;

@Mapper(componentModel = "spring")
public interface WhatsappAccountPersistenceMapper {
    WhatsappAccount toDomain(WhatsappAccountJpaEntity entity);

    WhatsappAccountJpaEntity toEntity(WhatsappAccount domain);
}
