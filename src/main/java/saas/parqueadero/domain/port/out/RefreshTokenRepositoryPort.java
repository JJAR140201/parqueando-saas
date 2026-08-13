package saas.parqueadero.domain.port.out;

import java.util.Optional;
import saas.parqueadero.domain.model.RefreshToken;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllActiveByUsuarioId(Long usuarioId);
}
