package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.IssueLicenciaRequest;
import saas.parqueadero.application.dto.LicenciaIssuedResponse;
import saas.parqueadero.application.dto.LicenciaSummaryResponse;
import saas.parqueadero.domain.port.in.LicenciaUseCase;

@RestController
@RequestMapping("/api/v1/super-admin/licencias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Licencias", description = "Emision y administracion de licencias anuales de clientes")
@SecurityRequirement(name = "bearerAuth")
public class LicenciaAdminController {

    private final LicenciaUseCase licenciaUseCase;

    @PostMapping
    @Operation(summary = "Emitir una nueva licencia", responses = {
        @ApiResponse(responseCode = "201", description = "Licencia emitida"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<LicenciaIssuedResponse> issue(@Valid @RequestBody IssueLicenciaRequest request) {
        log.info("[LicenciaAdminController] Emitir licencia duracionDias={}", request.getDuracionDias());
        return ResponseEntity.status(HttpStatus.CREATED).body(licenciaUseCase.issue(request));
    }

    @GetMapping
    @Operation(summary = "Listar licencias", responses = {
        @ApiResponse(responseCode = "200", description = "Lista de licencias")
    })
    public ResponseEntity<List<LicenciaSummaryResponse>> list() {
        log.info("[LicenciaAdminController] Listar licencias");
        return ResponseEntity.ok(licenciaUseCase.list());
    }

    @PostMapping("/{licenciaId}/revocar")
    @Operation(summary = "Revocar una licencia", responses = {
        @ApiResponse(responseCode = "200", description = "Licencia revocada"),
        @ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<LicenciaSummaryResponse> revoke(@PathVariable Long licenciaId) {
        log.info("[LicenciaAdminController] Revocar licencia id={}", licenciaId);
        return ResponseEntity.ok(licenciaUseCase.revoke(licenciaId));
    }
}
