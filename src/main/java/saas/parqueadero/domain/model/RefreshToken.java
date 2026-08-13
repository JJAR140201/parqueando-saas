package saas.parqueadero.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private Long id;
    private Long usuarioId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;
}
