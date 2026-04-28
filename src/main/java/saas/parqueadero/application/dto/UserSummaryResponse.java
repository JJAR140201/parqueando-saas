package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long usuarioId;
    private String username;
    private String rol;
    private Long empresaId;
    private String empresaNombre;
    private Long sedeId;
    private String sedeNombre;
}