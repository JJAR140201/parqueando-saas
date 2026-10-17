package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.WhatsappMessageJpaEntity;

@Mapper(componentModel = "spring")
public interface WhatsappMessagePersistenceMapper {
    WhatsappMessage toDomain(WhatsappMessageJpaEntity entity);

    WhatsappMessageJpaEntity toEntity(WhatsappMessage domain);
}
