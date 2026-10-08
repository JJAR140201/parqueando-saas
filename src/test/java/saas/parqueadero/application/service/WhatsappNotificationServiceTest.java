package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappAccountStatus;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.in.MensualidadNotificacionUseCase.Resultado;
import saas.parqueadero.domain.port.out.ClienteRepositoryPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;
import saas.parqueadero.domain.port.out.TokenCipherPort;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappGateway;
import saas.parqueadero.domain.port.out.WhatsappGateway.Credenciales;
import saas.parqueadero.domain.port.out.WhatsappGateway.EnvioResultado;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

class WhatsappNotificationServiceTest {

    private static final long EMPRESA = 5L;

    private final SuscripcionMensualRepositoryPort suscripcionRepository = mock(SuscripcionMensualRepositoryPort.class);
    private final EmpresaRepositoryPort empresaRepository = mock(EmpresaRepositoryPort.class);
    private final ClienteRepositoryPort clienteRepository = mock(ClienteRepositoryPort.class);
    private final WhatsappAccountRepositoryPort accountRepository = mock(WhatsappAccountRepositoryPort.class);
    private final WhatsappMessageRepositoryPort messageRepository = mock(WhatsappMessageRepositoryPort.class);
    private final WhatsappGateway gateway = mock(WhatsappGateway.class);
    private final TokenCipherPort cipher = mock(TokenCipherPort.class);

    private final Instant ahora = Instant.parse("2026-10-07T15:00:00Z");
    private final Clock clock = Clock.fixed(ahora, ZoneId.of("America/Bogota"));

    private WhatsappNotificationService service;
    private WhatsappAccount cuenta;
    private Cliente cliente;
    private SuscripcionMensual suscripcion;

    @BeforeEach
    void setUp() {
        service = new WhatsappNotificationService(suscripcionRepository, empresaRepository, clienteRepository,
            accountRepository, messageRepository, gateway, cipher, new WhatsappMensajeFormatter(),
            new WhatsappProperties(null, null, "es", null, null, null, null, null), new ObjectMapper(), clock);

        cuenta = WhatsappAccount.builder().id(7L).empresaId(EMPRESA).phoneNumberId("555").enabled(true)
            .status(WhatsappAccountStatus.PENDIENTE).accessTokenEncrypted("v1:cifrado").build();
        cliente = Cliente.builder().id(3L).empresaId(EMPRESA).nombre("Juan").telefono("+573001234567")
            .whatsappHabilitado(true).prefFacturacion(true).prefMensualidades(true).prefConfirmacionesPago(true).build();
        suscripcion = SuscripcionMensual.builder().id(9L).empresaId(EMPRESA).clienteId(3L).telefono("+573001234567")
            .valorMensual(new BigDecimal("120000")).fechaInicio(LocalDate.of(2026, 10, 1)).fechaFin(LocalDate.of(2026, 10, 10)).build();

        when(suscripcionRepository.findById(9L)).thenReturn(Optional.of(suscripcion));
        when(empresaRepository.findById(EMPRESA)).thenReturn(Optional.of(Empresa.builder().id(EMPRESA).nombre("Parqueadero Central").build()));
        when(accountRepository.findByEmpresaId(EMPRESA)).thenReturn(Optional.of(cuenta));
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(messageRepository.save(any())).thenAnswer(inv -> {
            WhatsappMessage m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(100L);
            }
            return m;
        });
        when(cipher.descifrar("v1:cifrado")).thenReturn("TOKEN-REAL");
    }

    private WhatsappMessage ultimoMensajeGuardado() {
        ArgumentCaptor<WhatsappMessage> captor = ArgumentCaptor.forClass(WhatsappMessage.class);
        verify(messageRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    @Test
    void enviaLaPlantillaCorrectaConElTokenDescifradoYRegistraElMensaje() {
        when(gateway.enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(EnvioResultado.aceptado("wamid.XYZ"));

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.ENVIADA);
        ArgumentCaptor<Credenciales> credenciales = ArgumentCaptor.forClass(Credenciales.class);
        verify(gateway).enviarPlantilla(credenciales.capture(), eq("+573001234567"), eq("monthly_invoice_created"), eq("es"),
            eq(List.of("Juan", "octubre de 2026", "$120.000", "10 de octubre de 2026", "Parqueadero Central")));
        assertThat(credenciales.getValue().phoneNumberId()).isEqualTo("555");
        assertThat(credenciales.getValue().accessToken()).isEqualTo("TOKEN-REAL");

        WhatsappMessage guardado = ultimoMensajeGuardado();
        assertThat(guardado.getStatus()).isEqualTo(WhatsappMessageStatus.ACCEPTED);
        assertThat(guardado.getProviderMessageId()).isEqualTo("wamid.XYZ");
        assertThat(guardado.getEmpresaId()).isEqualTo(EMPRESA);
        assertThat(guardado.getClienteId()).isEqualTo(3L);
        assertThat(guardado.getSuscripcionId()).isEqualTo(9L);
        assertThat(guardado.getRequestedAt()).isEqualTo(ahora);
        assertThat(guardado.getProviderAcceptedAt()).isEqualTo(ahora);
        assertThat(cuenta.getStatus()).isEqualTo(WhatsappAccountStatus.ACTIVA);
    }

    @Test
    void cadaTipoUsaSuPlantilla() {
        when(gateway.enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(EnvioResultado.aceptado("wamid.1"));

        service.notificar(TipoNotificacionWhatsapp.RECORDATORIO_VENCIMIENTO, 9L);
        service.notificar(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, 9L);
        service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_VENCIDA, 9L);

        verify(gateway).enviarPlantilla(any(), anyString(), eq("monthly_payment_reminder"), anyString(), anyList());
        verify(gateway).enviarPlantilla(any(), anyString(), eq("payment_confirmation"), anyString(), anyList());
        verify(gateway).enviarPlantilla(any(), anyString(), eq("monthly_payment_overdue"), anyString(), anyList());
    }

    @Test
    void sinCuentaHabilitadaNoSeEnviaNiSeRegistraNada() {
        cuenta.setEnabled(false);

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.OMITIDA_SIN_CUENTA);
        verify(gateway, never()).enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sinConsentimientoDelClienteNoSeEnviaYQuedaRegistrado() {
        cliente.setWhatsappHabilitado(false);

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.OMITIDA_NO_PERMITIDA);
        verify(gateway, never()).enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList());
        WhatsappMessage guardado = ultimoMensajeGuardado();
        assertThat(guardado.getStatus()).isEqualTo(WhatsappMessageStatus.NOT_SENT);
        assertThat(guardado.getErrorCode()).isEqualTo("PREFERENCIA_DESHABILITADA");
    }

    @Test
    void respetaLaPreferenciaPorTipoDeMensaje() {
        cliente.setPrefConfirmacionesPago(false);

        assertThat(service.notificar(TipoNotificacionWhatsapp.CONFIRMACION_PAGO, 9L)).isEqualTo(Resultado.OMITIDA_NO_PERMITIDA);
        verify(gateway, never()).enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void conTelefonoInvalidoNoSeIntentaEnviar() {
        cliente.setTelefono("3001234567");

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.OMITIDA_TELEFONO_INVALIDO);
        verify(gateway, never()).enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList());
        WhatsappMessage guardado = ultimoMensajeGuardado();
        assertThat(guardado.getStatus()).isEqualTo(WhatsappMessageStatus.NOT_SENT);
        assertThat(guardado.getErrorCode()).isEqualTo("TELEFONO_INVALIDO");
    }

    @Test
    void siMetaRechazaElMensajeQuedaFallidoConSuCodigo() {
        when(gateway.enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(EnvioResultado.fallido("META_131030", "Recipient phone number not in allowed list"));

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.FALLIDA);
        WhatsappMessage guardado = ultimoMensajeGuardado();
        assertThat(guardado.getStatus()).isEqualTo(WhatsappMessageStatus.FAILED);
        assertThat(guardado.getErrorCode()).isEqualTo("META_131030");
        assertThat(guardado.getFailedAt()).isEqualTo(ahora);
        assertThat(guardado.getProviderMessageId()).isNull();
    }

    @Test
    void unTokenInvalidoMarcaLaCuentaEnError() {
        when(gateway.enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(EnvioResultado.fallido("META_190", "Invalid OAuth access token"));

        service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(cuenta.getStatus()).isEqualTo(WhatsappAccountStatus.ERROR);
    }

    @Test
    void cualquierExcepcionSeContieneYNoLlegaAlProcesoDeNegocio() {
        when(gateway.enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("boom"));

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.FALLIDA);
        assertThat(ultimoMensajeGuardado().getStatus()).isEqualTo(WhatsappMessageStatus.FAILED);
    }

    @Test
    void hastaUnaFallaDelRepositorioSeContiene() {
        when(suscripcionRepository.findById(9L)).thenThrow(new IllegalStateException("BD caida"));

        assertThat(service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L)).isEqualTo(Resultado.FALLIDA);
    }

    @Test
    void unClienteDeOtraEmpresaNuncaSeUsa() {
        cliente.setEmpresaId(99L);

        Resultado resultado = service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        assertThat(resultado).isEqualTo(Resultado.OMITIDA_NO_PERMITIDA);
        verify(gateway, never()).enviarPlantilla(any(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void usaSoloLaCuentaDeLaEmpresaDeLaMensualidad() {
        service.notificar(TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, 9L);

        verify(accountRepository).findByEmpresaId(EMPRESA);
        verify(accountRepository, never()).findByPhoneNumberId(anyString());
    }

    @Test
    void elTelefonoDeberiaSerE164() {
        assertThat(WhatsappNotificationService.telefonoValido("+573026088215")).isTrue();
        assertThat(WhatsappNotificationService.telefonoValido("573026088215")).isFalse();
        assertThat(WhatsappNotificationService.telefonoValido("+57 302 608 8215")).isFalse();
        assertThat(WhatsappNotificationService.telefonoValido(null)).isFalse();
        assertThat(ZoneOffset.UTC).isNotNull();
    }
}
