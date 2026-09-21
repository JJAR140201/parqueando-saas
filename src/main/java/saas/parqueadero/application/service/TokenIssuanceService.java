package saas.parqueadero.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.domain.model.RefreshToken;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.out.RefreshTokenRepositoryPort;
import saas.parqueadero.infrastructure.configuration.security.JwtProperties;
import saas.parqueadero.infrastructure.configuration.security.JwtTokenProvider;

/**
 * Emision de access/refresh tokens. Extraida de {@link AuthService} para poder reutilizarla
 * desde otros flujos que dejan al usuario logueado ademas del login normal (por ejemplo, la
 * activacion de una licencia que crea el usuario ADMIN y lo loguea de una vez).
 */
@Service
@RequiredArgsConstructor
public class TokenIssuanceService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 64;

    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public LoginResponse buildLoginResponse(Usuario usuario) {
        String accessToken = jwtTokenProvider.generateToken(usuario);
        String refreshToken = issueRefreshToken(usuario);

        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .usuarioId(usuario.getId())
            .nombre(usuario.getNombre())
            .empresaId(usuario.getEmpresaId())
            .sedeId(usuario.getSedeId())
            .username(usuario.getUsername())
            .rol(usuario.getRol().name())
            .build();
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private String issueRefreshToken(Usuario usuario) {
        String rawToken = generateOpaqueToken();
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepositoryPort.save(RefreshToken.builder()
            .usuarioId(usuario.getId())
            .tokenHash(hashToken(rawToken))
            .expiresAt(now.plus(Duration.ofMillis(resolveRefreshExpirationMillis(usuario.getRol()))))
            .revoked(false)
            .createdAt(now)
            .build());

        return rawToken;
    }

    private long resolveRefreshExpirationMillis(RolUsuario rol) {
        JwtProperties.RefreshExpirationMillis config = jwtProperties.refreshExpirationMillis();
        return switch (rol) {
            case SUPER_ADMIN -> config.superAdmin();
            case ADMIN -> config.admin();
            case OPERARIO -> config.operario();
        };
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
