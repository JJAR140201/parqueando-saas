package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.ClienteJpaEntity;

@Mapper(componentModel = "spring")
public interface ClientePersistenceMapper {
    Cliente toDomain(ClienteJpaEntity entity);

    ClienteJpaEntity toEntity(Cliente domain);
}
