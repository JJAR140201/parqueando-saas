package saas.parqueadero.application.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al dar de alta una instalacion. El {@code token} se muestra una sola
 * vez: hay que copiarlo al paquete de activacion del cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarClienteResponse {
    private String tenantId;
    private String token;
    private String syncUrl;
    private String nombre;
    private Long empresaId;
    private Instant creadaEn;
}
