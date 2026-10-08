package saas.parqueadero.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

class WhatsappWebhookServiceTest {

    private static final String SECRETO = "secreto-de-la-app";

    private final WhatsappAccountRepositoryPort accountRepository = mock(WhatsappAccountRepositoryPort.class);
    private final WhatsappMessageRepositoryPort messageRepository = mock(WhatsappMessageRepositoryPort.class);
    private WhatsappWebhookService service;
    private WhatsappMessage mensaje;

    @BeforeEach
    void setUp() {
        service = new WhatsappWebhookService(accountRepository, messageRepository,
            new WhatsappProperties(null, null, null, SECRETO, "verif-token", null, null, null), new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC));
        service.esperaReintentoMillis = 0;

        mensaje = WhatsappMessage.builder().id(1L).empresaId(5L).providerMessageId("wamid.A")
            .status(WhatsappMessageStatus.ACCEPTED).requestedAt(Instant.parse("2026-10-07T14:59:59Z")).build();
        when(accountRepository.findByPhoneNumberId("555")).thenReturn(Optional.of(WhatsappAccount.builder().id(7L).empresaId(5L).build()));
        when(messageRepository.findByProviderMessageId("wamid.A")).thenReturn(Optional.of(mensaje));
    }

    private static String firma(String cuerpo, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] evento(String phoneNumberId, String idMensaje, String estado, long timestamp, String errores) {
        return ("{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"field\":\"messages\",\"value\":{"
            + "\"metadata\":{\"phone_number_id\":\"" + phoneNumberId + "\"},\"statuses\":[{\"id\":\"" + idMensaje
            + "\",\"status\":\"" + estado + "\",\"timestamp\":\"" + timestamp + "\"" + errores + "}]}}]}]}")
            .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void aceptaUnaFirmaValida() throws Exception {
        String cuerpo = "{\"a\":1}";
        assertThat(service.firmaValida(cuerpo.getBytes(StandardCharsets.UTF_8), firma(cuerpo, SECRETO))).isTrue();
    }

    @Test
    void rechazaFirmasIncorrectasAusentesOMalFormadas() throws Exception {
        byte[] cuerpo = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        assertThat(service.firmaValida(cuerpo, firma("{\"a\":1}", "otro-secreto"))).isFalse();
        assertThat(service.firmaValida(cuerpo, firma("{\"a\":2}", SECRETO))).isFalse();
        assertThat(service.firmaValida(cuerpo, null)).isFalse();
        assertThat(service.firmaValida(cuerpo, "sha256=")).isFalse();
        assertThat(service.firmaValida(cuerpo, "md5=abc")).isFalse();
    }

    @Test
    void sinSecretoConfiguradoRechazaTodo() throws Exception {
        WhatsappWebhookService sinSecreto = new WhatsappWebhookService(accountRepository, messageRepository,
            new WhatsappProperties(null, null, null, "", null, null, null, null), new ObjectMapper(), Clock.systemUTC());

        // Aun con una firma que seria correcta para un secreto vacio, se rechaza
        assertThat(sinSecreto.firmaValida("x".getBytes(StandardCharsets.UTF_8),
            "sha256=b613679a0814d9ec772f95d778c35fc5ff1697c493715653c6c712144292c5ad")).isFalse();
    }

    @Test
    void verificaLaSuscripcionSoloConElTokenCorrecto() {
        assertThat(service.verificarSuscripcion("subscribe", "verif-token", "desafio-123")).isEqualTo("desafio-123");
        assertThat(service.verificarSuscripcion("subscribe", "otro", "desafio-123")).isNull();
        assertThat(service.verificarSuscripcion("unsubscribe", "verif-token", "desafio-123")).isNull();
        assertThat(service.verificarSuscripcion("subscribe", null, "desafio-123")).isNull();
    }

    @Test
    void elCicloSentDeliveredReadActualizaEstadosYFechas() {
        service.procesar(evento("555", "wamid.A", "sent", 1_790_000_001L, ""));
        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.SENT);
        assertThat(mensaje.getSentAt()).isEqualTo(Instant.ofEpochSecond(1_790_000_001L));

        service.procesar(evento("555", "wamid.A", "delivered", 1_790_000_002L, ""));
        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.DELIVERED);
        assertThat(mensaje.getDeliveredAt()).isEqualTo(Instant.ofEpochSecond(1_790_000_002L));

        service.procesar(evento("555", "wamid.A", "read", 1_790_000_009L, ""));
        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.READ);
        assertThat(mensaje.getReadAt()).isEqualTo(Instant.ofEpochSecond(1_790_000_009L));
    }

    @Test
    void unEstadoAnteriorNoHaceRetrocederElMensaje() {
        mensaje.setStatus(WhatsappMessageStatus.READ);

        service.procesar(evento("555", "wamid.A", "delivered", 1_790_000_002L, ""));
        service.procesar(evento("555", "wamid.A", "sent", 1_790_000_001L, ""));

        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.READ);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void unEventoRepetidoEsIdempotente() {
        service.procesar(evento("555", "wamid.A", "sent", 1_790_000_001L, ""));
        service.procesar(evento("555", "wamid.A", "sent", 1_790_000_001L, ""));

        verify(messageRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void failedGuardaElErrorDeMeta() {
        service.procesar(evento("555", "wamid.A", "failed", 1_790_000_005L,
            ",\"errors\":[{\"code\":131026,\"title\":\"Message undeliverable\",\"message\":\"Message undeliverable\"}]"));

        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.FAILED);
        assertThat(mensaje.getErrorCode()).isEqualTo("META_131026");
        assertThat(mensaje.getErrorMessage()).isEqualTo("Message undeliverable");
        assertThat(mensaje.getFailedAt()).isEqualTo(Instant.ofEpochSecond(1_790_000_005L));
    }

    @Test
    void unFalloTardioNoPisaUnMensajeYaEntregado() {
        mensaje.setStatus(WhatsappMessageStatus.DELIVERED);

        service.procesar(evento("555", "wamid.A", "failed", 1_790_000_005L, ",\"errors\":[{\"code\":1}]"));

        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.DELIVERED);
    }

    @Test
    void ignoraEventosDeUnMensajeDeOtraEmpresa() {
        mensaje.setEmpresaId(99L);

        service.procesar(evento("555", "wamid.A", "delivered", 1_790_000_002L, ""));

        assertThat(mensaje.getStatus()).isEqualTo(WhatsappMessageStatus.ACCEPTED);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void ignoraNumerosSinCuentaYMensajesDesconocidos() {
        when(messageRepository.findByProviderMessageId("wamid.NOEXISTE")).thenReturn(Optional.empty());

        service.procesar(evento("999", "wamid.A", "delivered", 1_790_000_002L, ""));
        service.procesar(evento("555", "wamid.NOEXISTE", "delivered", 1_790_000_002L, ""));

        verify(messageRepository, never()).save(any());
    }

    @Test
    void ignoraLosMensajesEntrantesDelCliente() {
        byte[] entrante = ("{\"entry\":[{\"changes\":[{\"field\":\"messages\",\"value\":{\"metadata\":{\"phone_number_id\":\"555\"},"
            + "\"messages\":[{\"from\":\"573001234567\",\"text\":{\"body\":\"hola\"}}]}}]}]}").getBytes(StandardCharsets.UTF_8);

        service.procesar(entrante);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void uncuerpoQueNoEsJsonSeRechaza() {
        assertThatThrownBy(() -> service.procesar("no es json".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(BusinessException.class);
    }
}
