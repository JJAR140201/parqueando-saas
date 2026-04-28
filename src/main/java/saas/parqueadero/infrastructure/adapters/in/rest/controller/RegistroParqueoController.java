package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.RegistrarEntradaRequest;
import saas.parqueadero.application.dto.RegistrarSalidaRequest;
import saas.parqueadero.application.dto.RegistroParqueoResponse;
import saas.parqueadero.domain.port.in.RegistroParqueoUseCase;

@RestController
@RequestMapping("/api/v1/registros")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Registro de parqueo", description = "Operaciones de entrada y salida de vehiculos")
@SecurityRequirement(name = "bearerAuth")
public class RegistroParqueoController {

    private final RegistroParqueoUseCase registroParqueoUseCase;

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
}
