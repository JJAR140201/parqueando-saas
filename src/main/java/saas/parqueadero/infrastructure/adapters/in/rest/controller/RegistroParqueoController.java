package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.PrecioSalidaResponse;
import saas.parqueadero.application.dto.RegistrarEntradaRequest;
import saas.parqueadero.application.dto.RegistrarSalidaRequest;
import saas.parqueadero.application.dto.RegistroParqueoResponse;
import saas.parqueadero.application.service.TicketParqueoService;
import saas.parqueadero.domain.port.in.RegistroParqueoUseCase;

@RestController
@RequestMapping({"/api/v1/registros", "/api/v1/operaciones/parqueadero"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Registro de parqueo", description = "Operaciones de entrada y salida de vehiculos")
@SecurityRequirement(name = "bearerAuth")
public class RegistroParqueoController {

    private final RegistroParqueoUseCase registroParqueoUseCase;
    private final TicketParqueoService ticketParqueoService;

    @PostMapping("/entrada")
    @Operation(summary = "Registrar entrada de vehiculo", responses = {
        @ApiResponse(responseCode = "201", description = "Entrada registrada"),
        @ApiResponse(responseCode = "400", description = "Error de validacion o negocio"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<RegistroParqueoResponse> registrarEntrada(@Valid @RequestBody RegistrarEntradaRequest request) {
        log.info("[RegistroParqueoController] Registrar entrada placa={} tipoVehiculo={}", request.getPlaca(), request.getTipoVehiculo());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registroParqueoUseCase.registrarEntrada(request));
    }

    @GetMapping("/salidas/precio")
    @Operation(summary = "Consultar precio de salida por placa", responses = {
        @ApiResponse(responseCode = "200", description = "Precio calculado"),
        @ApiResponse(responseCode = "400", description = "Error de validacion o negocio"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<PrecioSalidaResponse> consultarPrecioSalida(@RequestParam String placa) {
        log.info("[RegistroParqueoController] Consultar precio salida placa={}", placa);
        return ResponseEntity.ok(registroParqueoUseCase.consultarPrecioSalida(placa));
    }

    @PostMapping("/salida")
    @Operation(summary = "Registrar salida de vehiculo", responses = {
        @ApiResponse(responseCode = "200", description = "Salida registrada"),
        @ApiResponse(responseCode = "400", description = "Error de validacion o negocio"),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public ResponseEntity<RegistroParqueoResponse> registrarSalida(@Valid @RequestBody RegistrarSalidaRequest request) {
        log.info("[RegistroParqueoController] Registrar salida placa={}", request.getPlaca());
        return ResponseEntity.ok(registroParqueoUseCase.registrarSalida(request));
    }

    @GetMapping(value = "/salidas/ticket", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generar ticket/recibo de parqueo en PDF para imprimir", responses = {
        @ApiResponse(responseCode = "200", description = "Ticket PDF generado"),
        @ApiResponse(responseCode = "404", description = "No existe registro activo para la placa")
    })
    public ResponseEntity<byte[]> generarTicket(@RequestParam String placa) {
        log.info("[RegistroParqueoController] Generar ticket placa={}", placa);
        PrecioSalidaResponse precio = registroParqueoUseCase.consultarPrecioSalida(placa);
        byte[] pdf = ticketParqueoService.generarTicket(precio);
        String filename = "ticket-" + placa.toUpperCase() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
