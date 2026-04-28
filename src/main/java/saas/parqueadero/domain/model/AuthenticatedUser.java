package saas.parqueadero.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {
    private Long usuarioId;
    private Long empresaId;
    private Long sedeId;
    private String username;
    private List<String> roles;
}
