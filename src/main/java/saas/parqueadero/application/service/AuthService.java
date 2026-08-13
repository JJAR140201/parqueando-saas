package saas.parqueadero.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.LoginRequest;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.RefreshTokenRequest;
import saas.parqueadero.application.dto.RegisterUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.RefreshToken;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.AuthUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.RefreshTokenRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;
import saas.parqueadero.infrastructure.configuration.security.JwtProperties;
import saas.parqueadero.infrastructure.configuration.security.JwtTokenProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthUseCase {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 64;

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("[AuthService] Login iniciado username={}", request.getUsername());
        Usuario usuario = usuarioRepositoryPort.findByUsername(request.getUsername().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        log.debug("[AuthService] Usuario encontrado id={} rol={} empresaId={} sedeId={}",
            usuario.getId(), usuario.getRol(), usuario.getEmpresaId(), usuario.getSedeId());

        if (!isPasswordValid(request.getPassword(), usuario.getPassword())) {
            log.warn("[AuthService] Credenciales invalidas username={}", request.getUsername());
            throw new BusinessException("Credenciales invalidas");
        }

        return buildLoginResponse(usuario);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken existing = refreshTokenRepositoryPort.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BusinessException("Refresh token invalido"));

        if (existing.isRevoked()) {
            log.warn("[AuthService] Reuso de refresh token detectado usuarioId={}, revocando todas sus sesiones", existing.getUsuarioId());
            refreshTokenRepositoryPort.revokeAllActiveByUsuarioId(existing.getUsuarioId());
            throw new BusinessException("Refresh token invalido");
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expirado, inicia sesion nuevamente");
        }

        Usuario usuario = usuarioRepositoryPort.findById(existing.getUsuarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        existing.setRevoked(true);
        refreshTokenRepositoryPort.save(existing);

        log.info("[AuthService] Access token renovado usuarioId={}", usuario.getId());
        return buildLoginResponse(usuario);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepositoryPort.findByTokenHash(tokenHash)
            .ifPresent(existing -> {
                existing.setRevoked(true);
                refreshTokenRepositoryPort.save(existing);
                log.info("[AuthService] Logout, refresh token revocado usuarioId={}", existing.getUsuarioId());
            });
    }

    private LoginResponse buildLoginResponse(Usuario usuario) {
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

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private boolean isPasswordValid(String rawPassword, String storedPassword) {
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    @Override
    public RegisterUserResponse register(RegisterUserRequest request) {
        log.info("[AuthService] Registro usuario iniciado username={} rol={}", request.getUsername(), request.getRol());
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        RolUsuario creatorRole = resolveMainRole(currentUser);
        if (request.getRol() == null) {
            throw new BusinessException("El campo rol es obligatorio");
        }

        if (creatorRole == RolUsuario.OPERARIO) {
            throw new BusinessException("El rol OPERARIO no puede registrar usuarios");
        }
        if (creatorRole == RolUsuario.ADMIN && request.getRol() == RolUsuario.SUPER_ADMIN) {
            throw new BusinessException("El rol ADMIN no puede crear usuarios SUPER_ADMIN");
        }

        String normalizedUsername = request.getUsername().trim();
        String normalizedNombre = request.getNombre() == null || request.getNombre().isBlank()
            ? normalizedUsername
            : request.getNombre().trim();

        if (request.getRol() == RolUsuario.SUPER_ADMIN) {
            if (creatorRole != RolUsuario.SUPER_ADMIN) {
                throw new BusinessException("Solo SUPER_ADMIN puede crear usuarios SUPER_ADMIN");
            }
            if (request.getEmpresaId() != null || request.getSedeId() != null) {
                throw new BusinessException("Un SUPER_ADMIN no debe tener empresaId ni sedeId");
            }

            usuarioRepositoryPort.findByUsername(normalizedUsername)
                .ifPresent(existing -> {
                    throw new BusinessException("Ya existe un usuario con ese username");
                });

            Usuario createdSuperAdmin = usuarioRepositoryPort.save(Usuario.builder()
                .nombre(normalizedNombre)
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(RolUsuario.SUPER_ADMIN)
                .sedeId(null)
                .empresaId(null)
                .build());
            log.info("[AuthService] SUPER_ADMIN creado id={} username={}", createdSuperAdmin.getId(), createdSuperAdmin.getUsername());

            return RegisterUserResponse.builder()
                .usuarioId(createdSuperAdmin.getId())
                .nombre(createdSuperAdmin.getNombre())
                .username(createdSuperAdmin.getUsername())
                .rol(createdSuperAdmin.getRol().name())
                .empresaId(createdSuperAdmin.getEmpresaId())
                .sedeId(createdSuperAdmin.getSedeId())
                .build();
        }

        Long targetEmpresaId = resolveTargetEmpresaId(creatorRole, currentUser, request.getEmpresaId());
        if (request.getSedeId() == null) {
            throw new BusinessException("El campo sedeId es obligatorio para usuarios de empresa");
        }

        Sede sede = sedeRepositoryPort.findByIdAndEmpresaId(request.getSedeId(), targetEmpresaId)
            .orElseThrow(() -> new ResourceNotFoundException("La sede no existe para la empresa actual"));

        usuarioRepositoryPort.findByUsernameAndEmpresaId(normalizedUsername, targetEmpresaId)
            .ifPresent(existing -> {
                throw new BusinessException("Ya existe un usuario con ese username en la empresa");
            });

        Usuario toCreate = Usuario.builder()
            .nombre(normalizedNombre)
            .username(normalizedUsername)
            .password(passwordEncoder.encode(request.getPassword()))
            .rol(request.getRol())
            .sedeId(sede.getId())
            .empresaId(targetEmpresaId)
            .build();

        Usuario created = usuarioRepositoryPort.save(toCreate);
        log.info("[AuthService] Usuario creado id={} username={} rol={} empresaId={} sedeId={}",
            created.getId(), created.getUsername(), created.getRol(), created.getEmpresaId(), created.getSedeId());

        return RegisterUserResponse.builder()
            .usuarioId(created.getId())
            .nombre(created.getNombre())
            .username(created.getUsername())
            .rol(created.getRol().name())
            .empresaId(created.getEmpresaId())
            .sedeId(created.getSedeId())
            .build();
    }

    private Long resolveTargetEmpresaId(RolUsuario creatorRole, AuthenticatedUser currentUser, Long empresaIdFromRequest) {
        if (creatorRole == RolUsuario.SUPER_ADMIN) {
            if (empresaIdFromRequest == null) {
                throw new BusinessException("El campo empresaId es obligatorio para SUPER_ADMIN");
            }
            return empresaIdFromRequest;
        }

        if (empresaIdFromRequest != null && !empresaIdFromRequest.equals(currentUser.getEmpresaId())) {
            throw new BusinessException("No tiene permisos para crear usuarios en otra empresa");
        }

        return currentUser.getEmpresaId();
    }

    private RolUsuario resolveMainRole(AuthenticatedUser currentUser) {
        if (currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }

        String role = currentUser.getRoles().get(0).replace("ROLE_", "");
        try {
            return RolUsuario.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Rol no valido en el token");
        }
    }
}
