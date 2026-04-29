package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.RolUsuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    private String nombre;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private RolUsuario rol;

    private Long empresaId;

    private Long sedeId;
}
