package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.BrandingResponse;
import saas.parqueadero.application.dto.UpdateBrandingRequest;
import saas.parqueadero.domain.port.in.BrandingUseCase;

@RestController
@RequiredArgsConstructor
@Tag(name = "Branding", description = "Color y logo por empresa")
@SecurityRequirement(name = "bearerAuth")
public class BrandingController {

    private final BrandingUseCase brandingUseCase;

    @GetMapping("/api/v1/branding")
    @Operation(summary = "Marca de la empresa del usuario autenticado", responses = {
        @ApiResponse(responseCode = "200", description = "Color y logo (pueden venir vacios)")
    })
    public ResponseEntity<BrandingResponse> getMine() {
        return ResponseEntity.ok(brandingUseCase.getMine());
    }

    @PutMapping("/api/v1/super-admin/empresas/{empresaId}/branding")
    @Operation(summary = "Configurar color y logo de una empresa", responses = {
        @ApiResponse(responseCode = "200", description = "Marca actualizada"),
        @ApiResponse(responseCode = "400", description = "Validacion"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<BrandingResponse> update(
        @PathVariable Long empresaId,
        @Valid @RequestBody UpdateBrandingRequest request
    ) {
        return ResponseEntity.ok(brandingUseCase.update(empresaId, request));
    }
}
