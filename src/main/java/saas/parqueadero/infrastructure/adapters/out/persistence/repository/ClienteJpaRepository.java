package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.ClienteJpaEntity;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {
    Optional<ClienteJpaEntity> findByEmpresaIdAndTelefono(Long empresaId, String telefono);

    List<ClienteJpaEntity> findByEmpresaIdOrderByNombreAsc(Long empresaId);
}
