package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private Long id;
    private Long empresaId;
    private String nombre;
    private String telefono;
    private boolean whatsappHabilitado;
    private boolean prefFacturacion;
    private boolean prefMensualidades;
    private boolean prefConfirmacionesPago;
    private boolean prefOperativas;
}
