package saas.parqueadero.infrastructure.adapters.out.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import saas.parqueadero.domain.port.out.WhatsappGateway;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

/**
 * Envia mensajes de plantilla por la Cloud API de WhatsApp (Meta). Nunca lanza excepciones y nunca
 * escribe el token en logs.
 */
@Component
@Slf4j
public class MetaWhatsappAdapter implements WhatsappGateway {

    private static final int MAX_ERROR_MESSAGE = 480;

    private final RestClient restClient;
    private final String apiVersion;

    public MetaWhatsappAdapter(WhatsappProperties properties, RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis()));
        this.restClient = builder.clone()
            .baseUrl(properties.graphBaseUrl())
            .requestFactory(factory)
            .build();
        this.apiVersion = properties.apiVersion();
    }

    @Override
    public EnvioResultado enviarPlantilla(Credenciales credenciales, String destino, String plantilla, String idioma,
        List<String> variables) {
        try {
            JsonNode respuesta = restClient.post()
                .uri("/{version}/{phoneNumberId}/messages", apiVersion, credenciales.phoneNumberId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + credenciales.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo(destino, plantilla, idioma, variables))
                .retrieve()
                .body(JsonNode.class);

            String id = respuesta == null ? null : respuesta.path("messages").path(0).path("id").asText(null);
            if (id == null || id.isBlank()) {
                return EnvioResultado.fallido("PROVIDER_BAD_RESPONSE", "Meta respondio sin identificador de mensaje");
            }
            return EnvioResultado.aceptado(id);
        } catch (RestClientResponseException ex) {
            return errorDeMeta(ex);
        } catch (Exception ex) {
            log.warn("[MetaWhatsappAdapter] No se pudo contactar a Meta: {}", ex.getClass().getSimpleName());
            return EnvioResultado.fallido("PROVIDER_UNAVAILABLE", "No se pudo contactar a Meta: " + ex.getClass().getSimpleName());
        }
    }

    static Map<String, Object> cuerpo(String destino, String plantilla, String idioma, List<String> variables) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", plantilla);
        template.put("language", Map.of("code", idioma));
        if (variables != null && !variables.isEmpty()) {
            List<Map<String, String>> parametros = new ArrayList<>();
            for (String valor : variables) {
                parametros.add(Map.of("type", "text", "text", valor));
            }
            template.put("components", List.of(Map.of("type", "body", "parameters", parametros)));
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("to", destino.startsWith("+") ? destino.substring(1) : destino);
        cuerpo.put("type", "template");
        cuerpo.put("template", template);
        return cuerpo;
    }

    private EnvioResultado errorDeMeta(RestClientResponseException ex) {
        String codigo = "HTTP_" + ex.getStatusCode().value();
        String mensaje = "Meta respondio " + ex.getStatusCode().value();
        try {
            JsonNode error = new com.fasterxml.jackson.databind.ObjectMapper().readTree(ex.getResponseBodyAsString()).path("error");
            if (!error.isMissingNode()) {
                if (error.hasNonNull("code")) {
                    codigo = "META_" + error.get("code").asText();
                }
                if (error.hasNonNull("message")) {
                    mensaje = error.get("message").asText();
                }
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON: se conserva el codigo HTTP
        }
        log.warn("[MetaWhatsappAdapter] Meta rechazo el mensaje codigo={}", codigo);
        return EnvioResultado.fallido(codigo, mensaje.length() > MAX_ERROR_MESSAGE ? mensaje.substring(0, MAX_ERROR_MESSAGE) : mensaje);
    }
}
