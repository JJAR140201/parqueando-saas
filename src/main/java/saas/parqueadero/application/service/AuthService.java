package saas.parqueadero.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import saas.parqueadero.domain.exception.LicenciaInvalidaException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.EstadoLicencia;
import saas.parqueadero.domain.model.Licencia;
import saas.parqueadero.domain.model.RefreshToken;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Sede;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.AuthUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.LicenciaRepositoryPort;
import saas.parqueadero.domain.port.out.RefreshTokenRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final SedeRepositoryPort sedeRepositoryPort;
    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private final LicenciaRepositoryPort licenciaRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuanceService tokenIssuanceService;

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

        checkLicenciaActiva(usuario);

        return tokenIssuanceService.buildLoginResponse(usuario);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String tokenHash = tokenIssuanceService.hashToken(request.getRefreshToken());
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

        checkLicenciaActiva(usuario);

        existing.setRevoked(true);
        refreshTokenRepositoryPort.save(existing);

        log.info("[AuthService] Access token renovado usuarioId={}", usuario.getId());
        return tokenIssuanceService.buildLoginResponse(usuario);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = tokenIssuanceService.hashToken(request.getRefreshToken());
        refreshTokenRepositoryPort.findByTokenHash(tokenHash)
            .ifPresent(existing -> {
                existing.setRevoked(true);
                refreshTokenRepositoryPort.save(existing);
                log.info("[AuthService] Logout, refresh token revocado usuarioId={}", existing.getUsuarioId());
            });
    }

    /**
     * Empresas sin ninguna fila de {@link Licencia} (todas las creadas manualmente antes de
     * este feature) quedan exentas del bloqueo. Solo se bloquea el login/refresh si existe una
     * licencia asociada y esta REVOCADA o vencida.
     */
    private void checkLicenciaActiva(Usuario usuario) {
        if (usuario.getEmpresaId() == null) {
            return;
        }

        licenciaRepositoryPort.findByEmpresaId(usuario.getEmpresaId()).ifPresent(licencia -> {
            if (licencia.getEstado() == EstadoLicencia.REVOCADA) {
                throw new LicenciaInvalidaException("La licencia de tu empresa fue revocada. Contacta al proveedor.");
            }
            if (licencia.getFechaExpiracion().isBefore(LocalDate.now())) {
                throw new LicenciaInvalidaException("La licencia de tu empresa esta vencida. Contacta al proveedor para renovarla.");
            }
        });
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
