package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.RefreshToken;
import saas.parqueadero.domain.port.out.RefreshTokenRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.RefreshTokenPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.RefreshTokenJpaRepository;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return mapper.toDomain(refreshTokenJpaRepository.save(mapper.toEntity(refreshToken)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash)
            .map(mapper::toDomain);
    }

    @Override
    public void revokeAllActiveByUsuarioId(Long usuarioId) {
        refreshTokenJpaRepository.revokeAllActiveByUsuarioId(usuarioId);
    }
}
