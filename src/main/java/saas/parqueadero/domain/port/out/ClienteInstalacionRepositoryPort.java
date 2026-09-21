package saas.parqueadero.domain.port.out;

import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.ClienteInstalacion;

public interface ClienteInstalacionRepositoryPort {

    List<ClienteInstalacion> findAll();

    Optional<ClienteInstalacion> findByTenantId(String tenantId);

    boolean existsByEmpresaId(Long empresaId);

    ClienteInstalacion save(ClienteInstalacion cliente);
}
