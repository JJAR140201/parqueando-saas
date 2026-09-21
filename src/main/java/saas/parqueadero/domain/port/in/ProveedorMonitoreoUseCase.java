package saas.parqueadero.domain.port.in;

import java.util.List;
import saas.parqueadero.application.dto.ClienteInstalacionResponse;
import saas.parqueadero.application.dto.RegistrarClienteRequest;
import saas.parqueadero.application.dto.RegistrarClienteResponse;
import saas.parqueadero.application.dto.RotarTokenResponse;
import saas.parqueadero.application.dto.SnapshotDetalleResponse;

/**
 * Consola del proveedor (SUPER_ADMIN): dar de alta instalaciones y ver los datos
 * sincronizados de cada parqueadero cliente.
 */
public interface ProveedorMonitoreoUseCase {

    RegistrarClienteResponse registrarCliente(RegistrarClienteRequest request);

    List<ClienteInstalacionResponse> listarClientes();

    ClienteInstalacionResponse obtenerCliente(String tenantId);

    SnapshotDetalleResponse ultimoSnapshot(String tenantId);

    RotarTokenResponse rotarToken(String tenantId);

    ClienteInstalacionResponse cambiarEstado(String tenantId, boolean activa);
}
