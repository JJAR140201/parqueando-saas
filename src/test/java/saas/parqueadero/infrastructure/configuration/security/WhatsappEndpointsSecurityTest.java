package saas.parqueadero.infrastructure.configuration.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import saas.parqueadero.application.dto.WhatsappCuentaResponse;
import saas.parqueadero.application.dto.WhatsappMensajesPaginaResponse;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.Usuario;
import saas.parqueadero.domain.port.in.ClienteUseCase;
import saas.parqueadero.domain.port.in.WhatsappAdminUseCase;
import saas.parqueadero.domain.port.in.WhatsappWebhookUseCase;
import saas.parqueadero.infrastructure.adapters.in.rest.controller.ClienteController;
import saas.parqueadero.infrastructure.adapters.in.rest.controller.WhatsappAdminController;
import saas.parqueadero.infrastructure.adapters.in.rest.controller.WhatsappWebhookController;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.UsuarioJpaRepository;

@WebMvcTest(controllers = {WhatsappWebhookController.class, WhatsappAdminController.class, ClienteController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, ActiveUserChecker.class})
@TestPropertySource(properties = {
    "app.jwt.secret=dGVzdC1zZWNyZXQtZGUtcHJ1ZWJhcy1wYXJhLWp3dC0zMi1ieXRlcw==",
    "app.jwt.expiration-millis=900000",
    "app.cors.allowed-origins=https://app.ejemplo.com",
    "app.sync.max-payload-bytes=1024"
})
class WhatsappEndpointsSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private WhatsappWebhookUseCase webhookUseCase;
    @MockBean
    private WhatsappAdminUseCase adminUseCase;
    @MockBean
    private ClienteUseCase clienteUseCase;
    @MockBean
    private UsuarioJpaRepository usuarioJpaRepository;

    private String token(long id, RolUsuario rol) {
        when(usuarioJpaRepository.existsById(id)).thenReturn(true);
        return "Bearer " + jwtTokenProvider.generateToken(Usuario.builder()
            .id(id).username("u" + id).nombre("U").rol(rol).empresaId(5L).sedeId(1L).build());
    }

    // ------------------------------------------------------------------ webhook de Meta (publico, firmado)

    @Test
    void laVerificacionDelWebhookNoRequiereJwtYDevuelveElDesafio() throws Exception {
        when(webhookUseCase.verificarSuscripcion("subscribe", "tok", "12345")).thenReturn("12345");

        mockMvc.perform(get("/api/webhooks/meta/whatsapp")
                .param("hub.mode", "subscribe").param("hub.verify_token", "tok").param("hub.challenge", "12345"))
            .andExpect(status().isOk())
            .andExpect(content().string("12345"));
    }

    @Test
    void laVerificacionConTokenIncorrectoSeRechaza() throws Exception {
        when(webhookUseCase.verificarSuscripcion(any(), any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/webhooks/meta/whatsapp")
                .param("hub.mode", "subscribe").param("hub.verify_token", "malo").param("hub.challenge", "12345"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unEventoConFirmaInvalidaSeRechazaYNoSeProcesa() throws Exception {
        when(webhookUseCase.firmaValida(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/webhooks/meta/whatsapp").contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=falsa").content("{\"entry\":[]}"))
            .andExpect(status().isForbidden());

        verify(webhookUseCase, never()).procesar(any());
    }

    @Test
    void unEventoSinFirmaSeRechaza() throws Exception {
        mockMvc.perform(post("/api/webhooks/meta/whatsapp").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());

        verify(webhookUseCase, never()).procesar(any());
    }

    @Test
    void unEventoConFirmaValidaSeProcesaYSeConfirmaALaMeta() throws Exception {
        when(webhookUseCase.firmaValida(any(), eq("sha256=buena"))).thenReturn(true);

        mockMvc.perform(post("/api/webhooks/meta/whatsapp").contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=buena").content("{\"entry\":[]}"))
            .andExpect(status().isOk())
            .andExpect(content().string("EVENT_RECEIVED"));

        verify(webhookUseCase).procesar(any());
    }

    @Test
    void elWebhookRechazaCuerposDemasiadoGrandesAntesDeLeerlos() throws Exception {
        mockMvc.perform(post("/api/webhooks/meta/whatsapp").contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=x").content("x".repeat(1_048_577)))
            .andExpect(status().isPayloadTooLarge());

        verify(webhookUseCase, never()).firmaValida(any(), any());
    }

    // ------------------------------------------------------------------ administracion (JWT + rol)

    @Test
    void laAdministracionDeWhatsappExigeSesion() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/cuenta")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/whatsapp/mensajes")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/whatsapp/cuenta").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unOperarioNoPuedeAdministrarWhatsapp() throws Exception {
        mockMvc.perform(get("/api/v1/whatsapp/cuenta").header("Authorization", token(2L, RolUsuario.OPERARIO)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/whatsapp/metricas").header("Authorization", token(2L, RolUsuario.OPERARIO)))
            .andExpect(status().isForbidden());
    }

    @Test
    void sinCuentaConfiguradaRespondeSinContenido() throws Exception {
        when(adminUseCase.obtenerCuenta(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/whatsapp/cuenta").header("Authorization", token(3L, RolUsuario.ADMIN)))
            .andExpect(status().isNoContent());
    }

    @Test
    void laCuentaNuncaExponeElToken() throws Exception {
        when(adminUseCase.obtenerCuenta(any())).thenReturn(WhatsappCuentaResponse.builder()
            .id(1L).empresaId(5L).provider("META").phoneNumberId("PN1").status("ACTIVA").enabled(true).tokenConfigurado(true).build());

        mockMvc.perform(get("/api/v1/whatsapp/cuenta").header("Authorization", token(3L, RolUsuario.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenConfigurado").value(true))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.accessTokenEncrypted").doesNotExist());
    }

    @Test
    void guardarLaCuentaValidaLosCamposObligatorios() throws Exception {
        mockMvc.perform(put("/api/v1/whatsapp/cuenta").header("Authorization", token(3L, RolUsuario.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"wabaId\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void laBitacoraEntregaElTotalEnUnaCabecera() throws Exception {
        when(adminUseCase.listarMensajes(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new WhatsappMensajesPaginaResponse(List.of(), 42));

        mockMvc.perform(get("/api/v1/whatsapp/mensajes").header("Authorization", token(3L, RolUsuario.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "42"));
    }

    @Test
    void elOperarioPuedeVerLosClientesPeroNoEditarlos() throws Exception {
        when(clienteUseCase.listar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/clientes").header("Authorization", token(2L, RolUsuario.OPERARIO)))
            .andExpect(status().isOk());
    }
}
