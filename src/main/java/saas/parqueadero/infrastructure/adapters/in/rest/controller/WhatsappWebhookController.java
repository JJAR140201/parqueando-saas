package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.domain.port.in.WhatsappWebhookUseCase;

/**
 * Webhook unico de Meta para todas las empresas. Es publico en la cadena de seguridad porque no usa JWT:
 * la autenticidad se comprueba con la firma HMAC (X-Hub-Signature-256) y con el token de verificacion.
 */
@RestController
@RequestMapping("/api/webhooks/meta/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class WhatsappWebhookController {

    private final WhatsappWebhookUseCase webhookUseCase;

    /** Verificacion de la URL que Meta hace al configurar el webhook. */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificar(
        @RequestParam(name = "hub.mode", required = false) String modo,
        @RequestParam(name = "hub.verify_token", required = false) String token,
        @RequestParam(name = "hub.challenge", required = false) String desafio
    ) {
        String respuesta = webhookUseCase.verificarSuscripcion(modo, token, desafio);
        if (respuesta == null) {
            log.warn("[WhatsappWebhookController] Verificacion de webhook rechazada");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(respuesta);
    }

    /** Eventos de estado de los mensajes. Se lee el cuerpo crudo porque la firma se calcula sobre el. */
    @PostMapping(consumes = MediaType.ALL_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> recibir(
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String firma,
        @RequestBody byte[] cuerpo
    ) {
        if (!webhookUseCase.firmaValida(cuerpo, firma)) {
            log.warn("[WhatsappWebhookController] Webhook con firma invalida");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        webhookUseCase.procesar(cuerpo);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
