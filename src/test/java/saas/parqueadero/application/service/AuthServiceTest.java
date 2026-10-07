package saas.parqueadero.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import saas.parqueadero.application.dto.LoginRequest;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.TooManyRequestsException;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.LicenciaRepositoryPort;
import saas.parqueadero.domain.port.out.RefreshTokenRepositoryPort;
import saas.parqueadero.domain.port.out.SedeRepositoryPort;
import saas.parqueadero.domain.port.out.UsuarioRepositoryPort;

class AuthServiceTest {

    private final UsuarioRepositoryPort usuarioRepository = mock(UsuarioRepositoryPort.class);
    private final LicenciaRepositoryPort licenciaRepository = mock(LicenciaRepositoryPort.class);
    private final TokenIssuanceService tokenIssuance = mock(TokenIssuanceService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(usuarioRepository, mock(SedeRepositoryPort.class),
            mock(AuthenticatedUserProviderPort.class), mock(RefreshTokenRepositoryPort.class),
            licenciaRepository, encoder, tokenIssuance, new LoginAttemptService());
        when(tokenIssuance.buildLoginResponse(any()))
            .thenAnswer(inv -> LoginResponse.builder().username(((Usuario) inv.getArgument(0)).getUsername())
                .usuarioId(((Usuario) inv.getArgument(0)).getId()).build());
    }

    private Usuario usuario(long id, String username, String clave, Long empresaId) {
        return Usuario.builder().id(id).username(username).password(encoder.encode(clave))
            .rol(empresaId == null ? RolUsuario.SUPER_ADMIN : RolUsuario.ADMIN).empresaId(empresaId).build();
    }

    private LoginRequest login(String username, String clave) {
        return LoginRequest.builder().username(username).password(clave).build();
    }

    @Test
    void usuarioInexistenteYClaveIncorrectaDanElMismoMensaje() {
        when(usuarioRepository.findAllByUsername("nadie")).thenReturn(List.of());
        when(usuarioRepository.findAllByUsername("admin")).thenReturn(List.of(usuario(1, "admin", "correcta1", null)));

        BusinessException inexistente = assertThrows(BusinessException.class,
            () -> service.login(login("nadie", "cualquiera")));
        BusinessException claveMala = assertThrows(BusinessException.class,
            () -> service.login(login("admin", "incorrecta")));

        assertEquals("Credenciales invalidas", inexistente.getMessage());
        assertEquals(inexistente.getMessage(), claveMala.getMessage());
        verify(tokenIssuance, never()).buildLoginResponse(any());
    }

    @Test
    void conUsernameRepetidoEntreEmpresasEntraElDeLaClaveCorrecta() {
        when(usuarioRepository.findAllByUsername("admin")).thenReturn(List.of(
            usuario(10, "admin", "clave-empresa-A", null),
            usuario(20, "admin", "clave-empresa-B", null)));

        LoginResponse response = service.login(login("admin", "clave-empresa-B"));

        assertEquals(20L, response.getUsuarioId());
    }

    @Test
    void tras5FallosElUsuarioQuedaBloqueadoAunConLaClaveCorrecta() {
        when(usuarioRepository.findAllByUsername("admin")).thenReturn(List.of(usuario(1, "admin", "correcta1", null)));

        for (int i = 0; i < LoginAttemptService.MAX_FALLOS; i++) {
            assertThrows(BusinessException.class, () -> service.login(login("admin", "mala")));
        }

        assertThrows(TooManyRequestsException.class, () -> service.login(login("admin", "correcta1")));
    }

    @Test
    void unLoginCorrectoReiniciaElContadorDeFallos() {
        when(usuarioRepository.findAllByUsername("admin")).thenReturn(List.of(usuario(1, "admin", "correcta1", null)));

        for (int i = 0; i < LoginAttemptService.MAX_FALLOS - 1; i++) {
            assertThrows(BusinessException.class, () -> service.login(login("admin", "mala")));
        }
        service.login(login("admin", "correcta1"));

        for (int i = 0; i < LoginAttemptService.MAX_FALLOS - 1; i++) {
            assertThrows(BusinessException.class, () -> service.login(login("admin", "mala")));
        }
        assertEquals(1L, service.login(login("admin", "correcta1")).getUsuarioId());
    }
}
