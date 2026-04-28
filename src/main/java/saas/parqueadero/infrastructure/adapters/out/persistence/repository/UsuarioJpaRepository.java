package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.UsuarioJpaEntity;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {
    List<UsuarioJpaEntity> findByEmpresaId(Long empresaId);

    Optional<UsuarioJpaEntity> findByUsername(String username);

    Optional<UsuarioJpaEntity> findByUsernameAndEmpresaId(String username, Long empresaId);

    boolean existsBySedeIdAndEmpresaId(Long sedeId, Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
