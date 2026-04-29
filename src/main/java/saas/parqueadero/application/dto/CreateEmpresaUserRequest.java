package saas.parqueadero.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.RolUsuario;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmpresaUserRequest {
    private String nombre;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private RolUsuario rol;

    @NotNull
    private Long empresaId;

    @NotNull
    private Long sedeId;
}
