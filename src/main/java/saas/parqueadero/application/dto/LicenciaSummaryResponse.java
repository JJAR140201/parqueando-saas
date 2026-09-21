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
public class LicenciaSummaryResponse {
    private Long id;
    private String codigoEnmascarado;
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDate fechaExpiracion;
    private LocalDateTime fechaRedencion;
    private LocalDateTime fechaRevocacion;
    private Long empresaId;
    private String empresaNombre;
    private String nota;
}
