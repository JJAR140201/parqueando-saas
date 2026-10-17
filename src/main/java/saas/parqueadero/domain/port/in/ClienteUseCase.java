package saas.parqueadero.domain.port.in;

import java.util.List;
import saas.parqueadero.application.dto.ClienteResponse;
import saas.parqueadero.application.dto.UpdateClienteRequest;

public interface ClienteUseCase {

    /** Clientes de la empresa del usuario (SUPER_ADMIN indica la empresa). */
    List<ClienteResponse> listar(Long empresaId);

    ClienteResponse actualizar(Long id, UpdateClienteRequest request);
}
