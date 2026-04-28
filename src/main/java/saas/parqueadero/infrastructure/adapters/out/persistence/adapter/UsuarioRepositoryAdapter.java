package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.UsuarioPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.UsuarioJpaRepository;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioJpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<Usuario> findByEmpresaId(Long empresaId) {
        return usuarioJpaRepository.findByEmpresaId(empresaId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return usuarioJpaRepository.findByUsername(username)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByUsernameAndEmpresaId(String username, Long empresaId) {
        return usuarioJpaRepository.findByUsernameAndEmpresaId(username, empresaId)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySedeIdAndEmpresaId(Long sedeId, Long empresaId) {
        return usuarioJpaRepository.existsBySedeIdAndEmpresaId(sedeId, empresaId);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return mapper.toDomain(usuarioJpaRepository.save(mapper.toEntity(usuario)));
    }

    @Override
    public void deleteById(Long id) {
        usuarioJpaRepository.deleteById(id);
    }

    @Override
    public void deleteByEmpresaId(Long empresaId) {
        usuarioJpaRepository.deleteByEmpresaId(empresaId);
    }
}
