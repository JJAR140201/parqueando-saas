package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.port.out.ClienteRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.ClientePersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.ClienteJpaRepository;

@Component
@RequiredArgsConstructor
public class ClienteRepositoryAdapter implements ClienteRepositoryPort {

    private final ClienteJpaRepository repository;
    private final ClientePersistenceMapper mapper;

    @Override
    public Optional<Cliente> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByEmpresaIdAndTelefono(Long empresaId, String telefono) {
        return repository.findByEmpresaIdAndTelefono(empresaId, telefono).map(mapper::toDomain);
    }

    @Override
    public List<Cliente> findByEmpresaId(Long empresaId) {
        return repository.findByEmpresaIdOrderByNombreAsc(empresaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Cliente> findAllByIds(Set<Long> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Cliente save(Cliente cliente) {
        return mapper.toDomain(repository.save(mapper.toEntity(cliente)));
    }
}
