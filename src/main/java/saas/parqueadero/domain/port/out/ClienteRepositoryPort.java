package saas.parqueadero.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import saas.parqueadero.domain.model.Cliente;

public interface ClienteRepositoryPort {
    Optional<Cliente> findById(Long id);

    Optional<Cliente> findByEmpresaIdAndTelefono(Long empresaId, String telefono);

    List<Cliente> findByEmpresaId(Long empresaId);

    List<Cliente> findAllByIds(Set<Long> ids);

    Cliente save(Cliente cliente);
}
