package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.SnapshotUploadRequest;
import saas.parqueadero.application.dto.SnapshotUploadResponse;
import saas.parqueadero.domain.port.in.SyncUseCase;

/**
 * Endpoint que consumen las instalaciones de escritorio para subir su estado.
 * Autenticacion por cabeceras {@code X-Tenant-Id} + {@code X-Sync-Token}
 * (no JWT); la ruta esta abierta en la cadena de seguridad.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sincronizacion", description = "Recepcion de snapshots de las instalaciones de escritorio")
public class SyncController {

    private final SyncUseCase syncUseCase;

    @PostMapping("/snapshot")
    @Operation(summary = "Subir un snapshot operativo", responses = {
        @ApiResponse(responseCode = "200", description = "Snapshot recibido"),
        @ApiResponse(responseCode = "400", description = "Cuerpo invalido"),
        @ApiResponse(responseCode = "401", description = "Credenciales de sincronizacion invalidas")
    })
    public ResponseEntity<SnapshotUploadResponse> recibirSnapshot(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
        @RequestHeader(value = "X-Sync-Token", required = false) String token,
        @Valid @RequestBody SnapshotUploadRequest request
    ) {
        log.info("[SyncController] Snapshot entrante tenantId={}", tenantId);
        return ResponseEntity.ok(syncUseCase.recibirSnapshot(tenantId, token, request));
    }
}
