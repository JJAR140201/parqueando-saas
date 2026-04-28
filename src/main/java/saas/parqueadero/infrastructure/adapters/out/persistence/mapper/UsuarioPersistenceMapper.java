package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.UsuarioJpaEntity;

@Mapper(componentModel = "spring")
public interface UsuarioPersistenceMapper {
    Usuario toDomain(UsuarioJpaEntity entity);

    UsuarioJpaEntity toEntity(Usuario domain);
}
