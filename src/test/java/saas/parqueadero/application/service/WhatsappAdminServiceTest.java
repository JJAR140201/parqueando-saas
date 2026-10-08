package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import saas.parqueadero.application.dto.UpsertWhatsappCuentaRequest;
import saas.parqueadero.application.dto.WhatsappCuentaResponse;
import saas.parqueadero.application.dto.WhatsappMetricasResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappAccountStatus;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.TokenCipherPort;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;

class WhatsappAdminServiceTest {

    private final WhatsappAccountRepositoryPort accountRepository = mock(WhatsappAccountRepositoryPort.class);
    private final WhatsappMessageRepositoryPort messageRepository = mock(WhatsappMessageRepositoryPort.class);
    private final EmpresaRepositoryPort empresaRepository = mock(EmpresaRepositoryPort.class);
    private final TokenCipherPort cipher = mock(TokenCipherPort.class);
    private final AuthenticatedUserProviderPort userProvider = mock(AuthenticatedUserProviderPort.class);
    private final WhatsappNotificationService notificationService = mock(WhatsappNotificationService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneId.of("America/Bogota"));

    private WhatsappAdminService service;

    @BeforeEach
    void setUp() {
        service = new WhatsappAdminService(accountRepository, messageRepository, empresaRepository, cipher,
            new EmpresaScopeResolver(userProvider), notificationService, clock);
        when(empresaRepository.findById(5L)).thenReturn(Optional.of(Empresa.builder().id(5L).nombre("Central").build()));
        when(cipher.estaConfigurado()).thenReturn(true);
        when(cipher.cifrar("TOKEN-NUEVO")).thenReturn("v1:CIFRADO");
        when(accountRepository.save(any())).thenAnswer(inv -> {
            WhatsappAccount a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });
    }

    private void usuario(String rol, Long empresaId) {
        when(userProvider.getCurrentUser()).thenReturn(AuthenticatedUser.builder()
            .usuarioId(1L).username("u").roles(List.of(rol)).empresaId(empresaId).build());
    }

    private UpsertWhatsappCuentaRequest solicitud(String token, boolean habilitada) {
        return UpsertWhatsappCuentaRequest.builder().wabaId("WABA1").businessId("BIZ1").phoneNumberId("PN1")
            .displayPhoneNumber("+57 302 608 8215").accessToken(token).enabled(habilitada).build();
    }

    @Test
    void crearGuardaElTokenCifradoYNuncaLoDevuelve() {
        usuario("ADMIN", 5L);

        WhatsappCuentaResponse respuesta = service.guardarCuenta(null, solicitud("TOKEN-NUEVO", true));

        ArgumentCaptor<WhatsappAccount> guardada = ArgumentCaptor.forClass(WhatsappAccount.class);
        verify(accountRepository).save(guardada.capture());
        assertThat(guardada.getValue().getAccessTokenEncrypted()).isEqualTo("v1:CIFRADO");
        assertThat(guardada.getValue().getEmpresaId()).isEqualTo(5L);
        assertThat(guardada.getValue().getProvider()).isEqualTo("META");
        assertThat(guardada.getValue().getStatus()).isEqualTo(WhatsappAccountStatus.PENDIENTE);

        assertThat(respuesta.isTokenConfigurado()).isTrue();
        assertThat(respuesta.toString()).doesNotContain("TOKEN-NUEVO").doesNotContain("CIFRADO");
    }

    @Test
    void alCrearElTokenEsObligatorio() {
        usuario("ADMIN", 5L);

        assertThatThrownBy(() -> service.guardarCuenta(null, solicitud(null, false))).isInstanceOf(BusinessException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void alActualizarSinTokenSeConservaElAnterior() {
        usuario("ADMIN", 5L);
        WhatsappAccount existente = WhatsappAccount.builder().id(1L).empresaId(5L).phoneNumberId("PN1")
            .accessTokenEncrypted("v1:VIEJO").status(WhatsappAccountStatus.ACTIVA).createdAt(Instant.EPOCH).build();
        when(accountRepository.findByEmpresaId(5L)).thenReturn(Optional.of(existente));

        service.guardarCuenta(null, solicitud(null, true));

        assertThat(existente.getAccessTokenEncrypted()).isEqualTo("v1:VIEJO");
        assertThat(existente.getStatus()).isEqualTo(WhatsappAccountStatus.ACTIVA);
        verify(cipher, never()).cifrar(any());
    }

    @Test
    void cambiarElTokenVuelveLaCuentaAPendiente() {
        usuario("ADMIN", 5L);
        WhatsappAccount existente = WhatsappAccount.builder().id(1L).empresaId(5L).phoneNumberId("PN1")
            .accessTokenEncrypted("v1:VIEJO").status(WhatsappAccountStatus.ERROR).createdAt(Instant.EPOCH).build();
        when(accountRepository.findByEmpresaId(5L)).thenReturn(Optional.of(existente));

        service.guardarCuenta(null, solicitud("TOKEN-NUEVO", true));

        assertThat(existente.getAccessTokenEncrypted()).isEqualTo("v1:CIFRADO");
        assertThat(existente.getStatus()).isEqualTo(WhatsappAccountStatus.PENDIENTE);
    }

    @Test
    void unPhoneNumberIdDeOtraEmpresaNoSePuedeUsar() {
        usuario("ADMIN", 5L);
        when(accountRepository.findByPhoneNumberId("PN1"))
            .thenReturn(Optional.of(WhatsappAccount.builder().id(2L).empresaId(77L).build()));

        assertThatThrownBy(() -> service.guardarCuenta(null, solicitud("TOKEN-NUEVO", true)))
            .isInstanceOf(BusinessException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void sinClaveDeCifradoEnElServidorNoSeGuardaToken() {
        usuario("ADMIN", 5L);
        when(cipher.estaConfigurado()).thenReturn(false);

        assertThatThrownBy(() -> service.guardarCuenta(null, solicitud("TOKEN-NUEVO", true)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("cifrado");
    }

    @Test
    void unAdminNoPuedeOperarSobreOtraEmpresa() {
        usuario("ADMIN", 5L);

        assertThatThrownBy(() -> service.obtenerCuenta(99L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.guardarCuenta(99L, solicitud("TOKEN-NUEVO", true))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.metricas(99L, null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void unOperarioNoPuedeAdministrarWhatsapp() {
        usuario("OPERARIO", 5L);

        assertThatThrownBy(() -> service.obtenerCuenta(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.listarMensajes(null, null, null, null, null, 0, 10)).isInstanceOf(BusinessException.class);
    }

    @Test
    void elSuperAdminDebeIndicarLaEmpresa() {
        usuario("SUPER_ADMIN", null);

        assertThatThrownBy(() -> service.obtenerCuenta(null)).isInstanceOf(BusinessException.class);
        when(accountRepository.findByEmpresaId(5L)).thenReturn(Optional.empty());
        assertThat(service.obtenerCuenta(5L)).isNull();
    }

    @Test
    void laBitacoraValidaLaPaginacion() {
        usuario("ADMIN", 5L);

        assertThatThrownBy(() -> service.listarMensajes(null, null, null, null, null, -1, 10)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.listarMensajes(null, null, null, null, null, 0, 0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.listarMensajes(null, null, null, null, null, 0, 201)).isInstanceOf(BusinessException.class);
    }

    @Test
    void calculaLasLatenciasDeLosMensajes() {
        usuario("ADMIN", 5L);
        Instant t0 = Instant.parse("2026-10-07T15:00:00Z");
        WhatsappMessage entregado = WhatsappMessage.builder().status(WhatsappMessageStatus.DELIVERED)
            .messageType(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA)
            .requestedAt(t0).providerAcceptedAt(t0.plusMillis(300)).sentAt(t0.plusMillis(1200)).deliveredAt(t0.plusMillis(3000)).build();
        WhatsappMessage soloAceptado = WhatsappMessage.builder().status(WhatsappMessageStatus.ACCEPTED)
            .messageType(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA)
            .requestedAt(t0).providerAcceptedAt(t0.plusMillis(500)).build();
        when(messageRepository.findParaMetricas(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(List.of(entregado, soloAceptado));

        WhatsappMetricasResponse metricas = service.metricas(null, null, null);

        assertThat(metricas.totalMensajes()).isEqualTo(2);
        assertThat(metricas.porEstado().get("DELIVERED")).isEqualTo(1);
        assertThat(metricas.porEstado().get("ACCEPTED")).isEqualTo(1);
        assertThat(metricas.latenciaApi().muestras()).isEqualTo(2);
        assertThat(metricas.latenciaApi().promedioMs()).isEqualTo(400);
        assertThat(metricas.latenciaEnvio().promedioMs()).isEqualTo(1200);
        assertThat(metricas.latenciaEntrega().promedioMs()).isEqualTo(3000);
        assertThat(metricas.latenciaEntrega().muestras()).isEqualTo(1);
    }

    @Test
    void percentilesPorRangoMasCercano() {
        List<Long> datos = List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L);

        assertThat(WhatsappAdminService.percentil(datos, 50)).isEqualTo(50);
        assertThat(WhatsappAdminService.percentil(datos, 95)).isEqualTo(100);
        assertThat(WhatsappAdminService.latencia(List.of()).muestras()).isZero();
        assertThat(WhatsappAdminService.latencia(List.of()).promedioMs()).isNull();
    }
}
