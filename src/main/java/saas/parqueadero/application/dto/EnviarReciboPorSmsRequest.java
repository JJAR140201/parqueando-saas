package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarReciboPorSmsRequest {
    /**
     * Número de teléfono en formato E.164 (ej: +573001234567)
     */
    private String numeroTelefono;

    /**
     * Placa del vehículo para el cual se desea enviar el recibo
     */
    private String placa;
}
