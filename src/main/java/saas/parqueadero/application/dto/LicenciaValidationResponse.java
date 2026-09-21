package saas.parqueadero.application.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaValidationResponse {
    private boolean valida;
    private String mensaje;
    private LocalDate fechaExpiracion;
}
