package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserResponse {
    private Long usuarioId;
    private String nombre;
    private String username;
    private String rol;
    private Long empresaId;
    private Long sedeId;
}
