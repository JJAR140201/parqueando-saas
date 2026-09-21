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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.ClienteInstalacionResponse;
import saas.parqueadero.application.dto.RegistrarClienteRequest;
import saas.parqueadero.application.dto.RegistrarClienteResponse;
import saas.parqueadero.application.dto.RotarTokenResponse;
import saas.parqueadero.application.dto.SnapshotDetalleResponse;
import saas.parqueadero.domain.port.in.ProveedorMonitoreoUseCase;

/**
 * Consola del proveedor (SUPER_ADMIN): alta de instalaciones cliente y consulta
 * de los datos que cada una sincroniza.
 */
@RestController
@RequestMapping("/api/v1/super-admin/clientes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Monitoreo de clientes", description = "Instalaciones de escritorio y sus datos sincronizados")
@SecurityRequirement(name = "bearerAuth")
public class ProveedorMonitoreoController {

    private final ProveedorMonitoreoUseCase proveedorMonitoreoUseCase;

    @PostMapping
    @Operation(summary = "Dar de alta una instalacion cliente", responses = {
        @ApiResponse(responseCode = "201", description = "Instalacion creada; el token se muestra una sola vez"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<RegistrarClienteResponse> registrar(@Valid @RequestBody RegistrarClienteRequest request) {
        log.info("[ProveedorMonitoreoController] Registrar instalacion nombre={} empresaId={}",
            request.getNombre(), request.getEmpresaId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(proveedorMonitoreoUseCase.registrarCliente(request));
    }

    @GetMapping
    @Operation(summary = "Listar instalaciones cliente")
    public ResponseEntity<List<ClienteInstalacionResponse>> listar() {
        return ResponseEntity.ok(proveedorMonitoreoUseCase.listarClientes());
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Ver una instalacion cliente", responses = {
        @ApiResponse(responseCode = "200", description = "Instalacion"),
        @ApiResponse(responseCode = "404", description = "Instalacion no encontrada")
    })
    public ResponseEntity<ClienteInstalacionResponse> obtener(@PathVariable String tenantId) {
        return ResponseEntity.ok(proveedorMonitoreoUseCase.obtenerCliente(tenantId));
    }

    @GetMapping("/{tenantId}/snapshot")
    @Operation(summary = "Ultimo snapshot sincronizado (volcado completo)", responses = {
        @ApiResponse(responseCode = "200", description = "Snapshot"),
        @ApiResponse(responseCode = "404", description = "Instalacion no encontrada o sin datos")
    })
    public ResponseEntity<SnapshotDetalleResponse> ultimoSnapshot(@PathVariable String tenantId) {
        return ResponseEntity.ok(proveedorMonitoreoUseCase.ultimoSnapshot(tenantId));
    }

    @PostMapping("/{tenantId}/rotar-token")
    @Operation(summary = "Generar un token de sincronizacion nuevo", responses = {
        @ApiResponse(responseCode = "200", description = "Token rotado; se muestra una sola vez"),
        @ApiResponse(responseCode = "404", description = "Instalacion no encontrada")
    })
    public ResponseEntity<RotarTokenResponse> rotarToken(@PathVariable String tenantId) {
        return ResponseEntity.ok(proveedorMonitoreoUseCase.rotarToken(tenantId));
    }

    @PatchMapping("/{tenantId}/estado")
    @Operation(summary = "Activar o desactivar una instalacion", responses = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Instalacion no encontrada")
    })
    public ResponseEntity<ClienteInstalacionResponse> cambiarEstado(
        @PathVariable String tenantId,
        @RequestParam boolean activa
    ) {
        log.info("[ProveedorMonitoreoController] Cambiar estado tenantId={} activa={}", tenantId, activa);
        return ResponseEntity.ok(proveedorMonitoreoUseCase.cambiarEstado(tenantId, activa));
    }
}
