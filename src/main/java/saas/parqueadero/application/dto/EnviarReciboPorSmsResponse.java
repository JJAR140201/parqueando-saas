package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarReciboPorSmsResponse {
    /**
     * true si el SMS fue enviado correctamente
     */
    private Boolean exitoso;

    /**
     * Mensaje de respuesta
     */
    private String mensaje;

    /**
     * Placa del vehículo
     */
    private String placa;
}
