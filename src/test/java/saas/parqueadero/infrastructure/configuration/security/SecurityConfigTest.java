package saas.parqueadero.infrastructure.configuration.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.AuthUseCase;
import saas.parqueadero.domain.port.in.SuperAdminUseCase;
import saas.parqueadero.infrastructure.adapters.in.rest.controller.AdminController;
import saas.parqueadero.infrastructure.adapters.in.rest.controller.AuthController;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.UsuarioJpaRepository;

@WebMvcTest(controllers = {AdminController.class, AuthController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, ActiveUserChecker.class})
@TestPropertySource(properties = {
    "app.jwt.secret=dGVzdC1zZWNyZXQtZGUtcHJ1ZWJhcy1wYXJhLWp3dC0zMi1ieXRlcw==",
    "app.jwt.expiration-millis=900000",
    "app.cors.allowed-origins=https://app.ejemplo.com,https://*.vercel.app",
    "app.sync.max-payload-bytes=1024"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SuperAdminUseCase superAdminUseCase;
    @MockBean
    private AuthUseCase authUseCase;
    @MockBean
    private UsuarioJpaRepository usuarioJpaRepository;

    private String token(long id, RolUsuario rol) {
        return jwtTokenProvider.generateToken(Usuario.builder()
            .id(id).username("u" + id).nombre("U").rol(rol).empresaId(1L).sedeId(1L).build());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios").param("empresaId", "1"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void operarioNoPuedeUsarRutasDeSuperAdmin() throws Exception {
        when(usuarioJpaRepository.existsById(2L)).thenReturn(true);
        mockMvc.perform(get("/api/v1/admin/usuarios").param("empresaId", "1")
                .header("Authorization", "Bearer " + token(2L, RolUsuario.OPERARIO)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operarioNoPuedeRegistrarUsuarios() throws Exception {
        when(usuarioJpaRepository.existsById(2L)).thenReturn(true);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"x\",\"password\":\"12345678\",\"rol\":\"OPERARIO\"}")
                .header("Authorization", "Bearer " + token(2L, RolUsuario.OPERARIO)))
            .andExpect(status().isForbidden());
    }

    @Test
    void superAdminExistenteAccede() throws Exception {
        when(usuarioJpaRepository.existsById(1L)).thenReturn(true);
        when(superAdminUseCase.listUsersByEmpresa(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/admin/usuarios").param("empresaId", "1")
                .header("Authorization", "Bearer " + token(1L, RolUsuario.SUPER_ADMIN)))
            .andExpect(status().isOk());
    }

    @Test
    void tokenDeUsuarioEliminadoResponde401() throws Exception {
        when(usuarioJpaRepository.existsById(3L)).thenReturn(false);
        mockMvc.perform(get("/api/v1/admin/usuarios").param("empresaId", "1")
                .header("Authorization", "Bearer " + token(3L, RolUsuario.SUPER_ADMIN)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPermiteSoloOrigenesConfigurados() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/auth/login")
                .header("Origin", "https://app.ejemplo.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://app.ejemplo.com"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/v1/auth/login")
                .header("Origin", "https://malicioso.com")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginSeLimitaPorIpTrasDiezPeticiones() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "9.9.9.9")
                    .content("{\"username\":\"a\",\"password\":\"b\"}"))
                .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "9.9.9.9")
                .content("{\"username\":\"a\",\"password\":\"b\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }
}
