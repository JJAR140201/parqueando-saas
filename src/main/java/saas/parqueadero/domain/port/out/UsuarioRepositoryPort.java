package saas.parqueadero.domain.port.out;

import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.Usuario;

public interface UsuarioRepositoryPort {
    Optional<Usuario> findById(Long id);

    List<Usuario> findByEmpresaId(Long empresaId);

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByUsernameAndEmpresaId(String username, Long empresaId);

    boolean existsBySedeIdAndEmpresaId(Long sedeId, Long empresaId);

    Usuario save(Usuario usuario);

    void deleteById(Long id);

    void deleteByEmpresaId(Long empresaId);
}
