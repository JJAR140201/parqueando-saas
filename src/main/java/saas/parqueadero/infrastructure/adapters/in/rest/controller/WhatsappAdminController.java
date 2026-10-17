package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.UpsertWhatsappCuentaRequest;
import saas.parqueadero.application.dto.WhatsappCuentaResponse;
import saas.parqueadero.application.dto.WhatsappMensajeResponse;
import saas.parqueadero.application.dto.WhatsappMensajesPaginaResponse;
import saas.parqueadero.application.dto.WhatsappMetricasResponse;
import saas.parqueadero.application.dto.WhatsappPruebaRequest;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.in.WhatsappAdminUseCase;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp", description = "Configuracion de la cuenta de WhatsApp Business, bitacora de mensajes y metricas")
@SecurityRequirement(name = "bearerAuth")
public class WhatsappAdminController {

    private final WhatsappAdminUseCase whatsappAdminUseCase;

    @GetMapping("/cuenta")
    @Operation(summary = "Ver la cuenta de WhatsApp de la empresa (sin el token). 204 si aun no esta configurada")
    public ResponseEntity<WhatsappCuentaResponse> obtenerCuenta(@RequestParam(required = false) Long empresaId) {
        WhatsappCuentaResponse cuenta = whatsappAdminUseCase.obtenerCuenta(empresaId);
        return cuenta == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(cuenta);
    }

    @PutMapping("/cuenta")
    @Operation(summary = "Crear o actualizar la cuenta de WhatsApp de la empresa")
    public ResponseEntity<WhatsappCuentaResponse> guardarCuenta(
        @RequestParam(required = false) Long empresaId,
        @Valid @RequestBody UpsertWhatsappCuentaRequest request
    ) {
        log.info("[WhatsappAdminController] Guardar cuenta de WhatsApp empresaId={}", empresaId);
        return ResponseEntity.ok(whatsappAdminUseCase.guardarCuenta(empresaId, request));
    }

    @PostMapping("/mensajes/prueba")
    @Operation(summary = "Enviar un mensaje de plantilla de ejemplo para validar la cuenta")
    public ResponseEntity<WhatsappMensajeResponse> enviarPrueba(
        @RequestParam(required = false) Long empresaId,
        @Valid @RequestBody WhatsappPruebaRequest request
    ) {
        return ResponseEntity.ok(whatsappAdminUseCase.enviarPrueba(empresaId, request));
    }

    @GetMapping("/mensajes")
    @Operation(summary = "Bitacora de mensajes, del mas reciente al mas antiguo. El total viene en X-Total-Count")
    public ResponseEntity<List<WhatsappMensajeResponse>> listarMensajes(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) WhatsappMessageStatus estado,
        @RequestParam(required = false) TipoNotificacionWhatsapp tipo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        WhatsappMensajesPaginaResponse pagina =
            whatsappAdminUseCase.listarMensajes(empresaId, estado, tipo, desde, hasta, page, size);
        return ResponseEntity.ok()
            .header("X-Total-Count", String.valueOf(pagina.total()))
            .header("X-Page", String.valueOf(page))
            .header("X-Size", String.valueOf(size))
            .body(pagina.items());
    }

    @GetMapping("/metricas")
    @Operation(summary = "Conteo por estado y latencias (API, envio y entrega) del rango indicado")
    public ResponseEntity<WhatsappMetricasResponse> metricas(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(whatsappAdminUseCase.metricas(empresaId, desde, hasta));
    }
}
