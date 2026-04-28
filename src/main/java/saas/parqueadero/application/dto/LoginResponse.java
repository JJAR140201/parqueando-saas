package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long usuarioId;
    private Long empresaId;
    private Long sedeId;
    private String username;
    private String rol;
}
