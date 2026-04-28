package saas.parqueadero.infrastructure.adapters.out.persistence.mapper;

import org.mapstruct.Mapper;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.EmpresaJpaEntity;

@Mapper(componentModel = "spring")
public interface EmpresaPersistenceMapper {
    Empresa toDomain(EmpresaJpaEntity entity);

    EmpresaJpaEntity toEntity(Empresa domain);
}
