package saas.parqueadero.infrastructure.adapters.out.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import saas.parqueadero.domain.port.out.WhatsappGateway.Credenciales;
import saas.parqueadero.domain.port.out.WhatsappGateway.EnvioResultado;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

/** Prueba el adaptador contra un servidor HTTP local que imita a Graph API. */
class MetaWhatsappAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer servidor;
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.ABC123\"}]}";

    @BeforeEach
    void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            ruta.set(exchange.getRequestURI().getPath());
            autorizacion.set(exchange.getRequestHeaders().getFirst("Authorization"));
            cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private MetaWhatsappAdapter adaptador(String baseUrl) {
        return new MetaWhatsappAdapter(
            new WhatsappProperties(baseUrl, "v23.0", "es", null, null, null, 1000, 2000), RestClient.builder());
    }

    private MetaWhatsappAdapter adaptador() {
        return adaptador("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    void enviaLaPlantillaConLasVariablesYDevuelveElIdDeMeta() throws Exception {
        EnvioResultado resultado = adaptador().enviarPlantilla(new Credenciales("1122334455", "TOKEN-X"),
            "+573026088215", "monthly_invoice_created", "es", List.of("Juan", "octubre de 2026", "$120.000", "10 de octubre de 2026", "Central"));

        assertThat(resultado.aceptado()).isTrue();
        assertThat(resultado.providerMessageId()).isEqualTo("wamid.ABC123");
        assertThat(ruta.get()).isEqualTo("/v23.0/1122334455/messages");
        assertThat(autorizacion.get()).isEqualTo("Bearer TOKEN-X");

        JsonNode enviado = mapper.readTree(cuerpo.get());
        assertThat(enviado.path("messaging_product").asText()).isEqualTo("whatsapp");
        assertThat(enviado.path("to").asText()).isEqualTo("573026088215");
        assertThat(enviado.path("type").asText()).isEqualTo("template");
        assertThat(enviado.path("template").path("name").asText()).isEqualTo("monthly_invoice_created");
        assertThat(enviado.path("template").path("language").path("code").asText()).isEqualTo("es");
        JsonNode parametros = enviado.path("template").path("components").path(0).path("parameters");
        assertThat(parametros).hasSize(5);
        assertThat(parametros.path(0).path("text").asText()).isEqualTo("Juan");
        assertThat(parametros.path(2).path("text").asText()).isEqualTo("$120.000");
    }

    @Test
    void traduceElErrorDeMetaSinLanzarExcepciones() {
        estado = 401;
        respuesta = "{\"error\":{\"message\":\"Invalid OAuth access token.\",\"type\":\"OAuthException\",\"code\":190}}";

        EnvioResultado resultado = adaptador().enviarPlantilla(new Credenciales("1", "malo"), "+573001234567", "p", "es", List.of());

        assertThat(resultado.aceptado()).isFalse();
        assertThat(resultado.errorCode()).isEqualTo("META_190");
        assertThat(resultado.errorMessage()).contains("Invalid OAuth access token");
    }

    @Test
    void unaRespuestaSinIdSeTrataComoFallo() {
        respuesta = "{\"messaging_product\":\"whatsapp\"}";

        EnvioResultado resultado = adaptador().enviarPlantilla(new Credenciales("1", "t"), "+573001234567", "p", "es", List.of());

        assertThat(resultado.aceptado()).isFalse();
        assertThat(resultado.errorCode()).isEqualTo("PROVIDER_BAD_RESPONSE");
    }

    @Test
    void errorDelServidorDeMetaNoSePropaga() {
        estado = 503;
        respuesta = "<html>Service Unavailable</html>";

        EnvioResultado resultado = adaptador().enviarPlantilla(new Credenciales("1", "t"), "+573001234567", "p", "es", List.of());

        assertThat(resultado.aceptado()).isFalse();
        assertThat(resultado.errorCode()).isEqualTo("HTTP_503");
    }

    @Test
    void metaInalcanzableNoLanzaExcepcion() {
        EnvioResultado resultado = adaptador("http://127.0.0.1:1")
            .enviarPlantilla(new Credenciales("1", "t"), "+573001234567", "p", "es", List.of());

        assertThat(resultado.aceptado()).isFalse();
        assertThat(resultado.errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
    }
}
