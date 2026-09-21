package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import saas.parqueadero.application.dto.LicenciaRedemptionRequest;
import saas.parqueadero.application.dto.LicenciaValidationResponse;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.ValidateLicenciaRequest;
import saas.parqueadero.domain.port.in.LicenciaUseCase;

/**
 * Endpoints publicos (sin JWT) para que un cliente nuevo active la licencia que le entrego
 * el SUPER_ADMIN y cree su empresa/sede/usuario ADMIN. Ver {@link LicenciaAdminController}
 * para la emision, que si requiere autenticacion SUPER_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/licencias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Activacion de licencias", description = "Validacion y canje publico del codigo de licencia")
public class LicenciaPublicController {

    private final LicenciaUseCase licenciaUseCase;

    @PostMapping("/validar")
    @Operation(summary = "Validar un codigo de licencia", responses = {
        @ApiResponse(responseCode = "200", description = "Resultado de la validacion")
    })
    public ResponseEntity<LicenciaValidationResponse> validate(@Valid @RequestBody ValidateLicenciaRequest request) {
        log.info("[LicenciaPublicController] Validar codigo de licencia");
        return ResponseEntity.ok(licenciaUseCase.validate(request));
    }

    @PostMapping("/redimir")
    @Operation(summary = "Canjear una licencia y crear la empresa/sede/usuario ADMIN", responses = {
        @ApiResponse(responseCode = "201", description = "Licencia canjeada, sesion iniciada"),
        @ApiResponse(responseCode = "400", description = "Codigo invalido, vencido, revocado o ya utilizado")
    })
    public ResponseEntity<LoginResponse> redeem(@Valid @RequestBody LicenciaRedemptionRequest request) {
        log.info("[LicenciaPublicController] Canjear licencia");
        return ResponseEntity.status(HttpStatus.CREATED).body(licenciaUseCase.redeem(request));
    }
}
