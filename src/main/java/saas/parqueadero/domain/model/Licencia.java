package saas.parqueadero.domain.model;

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
public class Licencia {
    private Long id;
    private String codigo;
    private EstadoLicencia estado;
    private LocalDate fechaExpiracion;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaRedencion;
    private LocalDateTime fechaRevocacion;
    private String nota;
    private Long empresaId;
    private Long emitidaPorUsuarioId;
    private Long version;
}
