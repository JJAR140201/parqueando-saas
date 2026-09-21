package saas.parqueadero.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaIssuedResponse {
    private Long id;
    private String codigo;
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDate fechaExpiracion;
    private String nota;
}
