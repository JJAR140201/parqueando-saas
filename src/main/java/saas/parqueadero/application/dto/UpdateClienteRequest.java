package saas.parqueadero.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Los campos nulos no se modifican. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClienteRequest {
    @Size(min = 1, max = 120)
    private String nombre;
    private Boolean whatsappHabilitado;
    private Boolean prefFacturacion;
    private Boolean prefMensualidades;
    private Boolean prefConfirmacionesPago;
    private Boolean prefOperativas;
}
