package saas.parqueadero.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.in.WhatsappWebhookUseCase;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

/**
 * Recibe los webhooks de Meta. Un unico endpoint sirve a todas las empresas: la empresa se
 * identifica por el {@code phone_number_id} del evento y se comprueba que el mensaje le pertenezca.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappWebhookService implements WhatsappWebhookUseCase {

    private static final String PREFIJO_FIRMA = "sha256=";
    private static final int INTENTOS_BUSQUEDA = 3;

    private final WhatsappAccountRepositoryPort accountRepository;
    private final WhatsappMessageRepositoryPort messageRepository;
    private final WhatsappProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** Espera entre busquedas de un mensaje aun no guardado; se ajusta en las pruebas. */
    long esperaReintentoMillis = 300;

    @Override
    public boolean firmaValida(byte[] cuerpo, String cabeceraFirma) {
        if (!properties.hasAppSecret()) {
            log.error("[WhatsappWebhookService] app.whatsapp.app-secret no esta configurado: se rechaza el webhook");
            return false;
        }
        if (cabeceraFirma == null || !cabeceraFirma.startsWith(PREFIJO_FIRMA)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.appSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = HexFormat.of().formatHex(mac.doFinal(cuerpo));
            String recibida = cabeceraFirma.substring(PREFIJO_FIRMA.length()).trim().toLowerCase();
            return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), recibida.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String verificarSuscripcion(String modo, String token, String desafio) {
        if (!properties.hasWebhookVerifyToken() || !"subscribe".equals(modo) || token == null) {
            return null;
        }
        boolean coincide = MessageDigest.isEqual(
            properties.webhookVerifyToken().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        return coincide ? desafio : null;
    }

    @Override
    public void procesar(byte[] cuerpo) {
        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(cuerpo);
        } catch (Exception ex) {
            throw new BusinessException("Cuerpo del webhook invalido");
        }
        for (JsonNode entrada : raiz.path("entry")) {
            for (JsonNode cambio : entrada.path("changes")) {
                if (!"messages".equals(cambio.path("field").asText())) {
                    continue;
                }
                JsonNode valor = cambio.path("value");
                String phoneNumberId = valor.path("metadata").path("phone_number_id").asText(null);
                if (phoneNumberId == null) {
                    continue;
                }
                Optional<WhatsappAccount> cuenta = accountRepository.findByPhoneNumberId(phoneNumberId);
                if (cuenta.isEmpty()) {
                    log.debug("[WhatsappWebhookService] Evento de un numero sin cuenta registrada");
                    continue;
                }
                for (JsonNode estado : valor.path("statuses")) {
                    procesarEstado(cuenta.get(), estado);
                }
            }
        }
    }

    private void procesarEstado(WhatsappAccount cuenta, JsonNode estado) {
        String idProveedor = estado.path("id").asText(null);
        WhatsappMessageStatus nuevo = traducir(estado.path("status").asText(""));
        if (idProveedor == null || nuevo == null) {
            return;
        }
        WhatsappMessage mensaje = buscarMensaje(idProveedor);
        if (mensaje == null) {
            log.warn("[WhatsappWebhookService] Estado {} para un mensaje desconocido", nuevo);
            return;
        }
        if (!cuenta.getEmpresaId().equals(mensaje.getEmpresaId())) {
            // El evento llego por el numero de otra empresa: se ignora
            log.warn("[WhatsappWebhookService] Estado de mensaje {} no corresponde a la empresa del numero", mensaje.getId());
            return;
        }

        Instant momento = instante(estado.path("timestamp").asText(null));
        if (aplicar(mensaje, nuevo, momento, estado.path("errors"))) {
            messageRepository.save(mensaje);
            log.info("[WhatsappWebhookService] Mensaje {} -> {}", mensaje.getId(), mensaje.getStatus());
        }
    }

    /** Aplica el estado solo si avanza en el ciclo de entrega. Devuelve si hubo cambios. */
    static boolean aplicar(WhatsappMessage mensaje, WhatsappMessageStatus nuevo, Instant momento, JsonNode errores) {
        WhatsappMessageStatus actual = mensaje.getStatus();
        if (nuevo == WhatsappMessageStatus.FAILED) {
            if (actual == WhatsappMessageStatus.FAILED || actual.esEntregaAvanzada()) {
                return false;
            }
            mensaje.setStatus(WhatsappMessageStatus.FAILED);
            mensaje.setFailedAt(momento);
            JsonNode primero = errores.path(0);
            if (!primero.isMissingNode()) {
                mensaje.setErrorCode("META_" + primero.path("code").asText("?"));
                String detalle = primero.path("message").asText(primero.path("title").asText(""));
                mensaje.setErrorMessage(detalle.length() > 480 ? detalle.substring(0, 480) : detalle);
            }
            return true;
        }
        if (nuevo.rango() <= actual.rango()) {
            return false;
        }
        mensaje.setStatus(nuevo);
        switch (nuevo) {
            case SENT -> mensaje.setSentAt(momento);
            case DELIVERED -> mensaje.setDeliveredAt(momento);
            case READ -> mensaje.setReadAt(momento);
            default -> { }
        }
        return true;
    }

    private static WhatsappMessageStatus traducir(String estadoMeta) {
        return switch (estadoMeta) {
            case "sent" -> WhatsappMessageStatus.SENT;
            case "delivered" -> WhatsappMessageStatus.DELIVERED;
            case "read" -> WhatsappMessageStatus.READ;
            case "failed" -> WhatsappMessageStatus.FAILED;
            default -> null;
        };
    }

    private Instant instante(String epochSegundos) {
        try {
            return epochSegundos == null ? clock.instant() : Instant.ofEpochSecond(Long.parseLong(epochSegundos));
        } catch (NumberFormatException ex) {
            return clock.instant();
        }
    }

    /**
     * Meta puede avisar del estado de un mensaje apenas lo acepta, antes de que terminemos de guardar su
     * identificador: se reintenta brevemente antes de darlo por desconocido.
     */
    private WhatsappMessage buscarMensaje(String idProveedor) {
        for (int intento = 1; intento <= INTENTOS_BUSQUEDA; intento++) {
            Optional<WhatsappMessage> encontrado = messageRepository.findByProviderMessageId(idProveedor);
            if (encontrado.isPresent()) {
                return encontrado.get();
            }
            if (intento < INTENTOS_BUSQUEDA) {
                try {
                    Thread.sleep(esperaReintentoMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }
}
