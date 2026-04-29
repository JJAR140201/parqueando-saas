package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.CreateSuscripcionMensualRequest;
import saas.parqueadero.application.dto.SuscripcionMensualResponse;
import saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest;
import saas.parqueadero.application.service.ExportSuscripcionesService;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.port.in.SuscripcionMensualUseCase;

@RestController
@RequestMapping("/api/v1/mensualidades")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mensualidades", description = "Gestion de suscripciones mensuales")
@SecurityRequirement(name = "bearerAuth")
public class SuscripcionMensualController {

    private final SuscripcionMensualUseCase suscripcionMensualUseCase;
    private final ExportSuscripcionesService exportSuscripcionesService;

    @PostMapping
    @Operation(summary = "Crear suscripcion mensual", responses = {
        @ApiResponse(responseCode = "201", description = "Suscripcion creada"),
        @ApiResponse(responseCode = "400", description = "Error de validacion o negocio")
    })
    public ResponseEntity<SuscripcionMensualResponse> create(@Valid @RequestBody CreateSuscripcionMensualRequest request) {
        log.info("[SuscripcionMensualController] Crear suscripcion placa={} empresaId={} sedeId={}",
            request.getPlaca(), request.getEmpresaId(), request.getSedeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(suscripcionMensualUseCase.createSuscripcion(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar suscripcion mensual", responses = {
        @ApiResponse(responseCode = "200", description = "Suscripcion actualizada"),
        @ApiResponse(responseCode = "400", description = "Error de validacion o negocio"),
        @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada")
    })
    public ResponseEntity<SuscripcionMensualResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateSuscripcionMensualRequest request) {
        log.info("[SuscripcionMensualController] Actualizar suscripcion id={}", id);
        return ResponseEntity.ok(suscripcionMensualUseCase.updateSuscripcion(id, request));
    }

    @GetMapping
    @Operation(summary = "Listar suscripciones mensuales", responses = {
        @ApiResponse(responseCode = "200", description = "Lista de suscripciones")
    })
    public ResponseEntity<List<SuscripcionMensualResponse>> list(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) String placa
    ) {
        log.info("[SuscripcionMensualController] Listar suscripciones empresaId={} sedeId={} placa={}", empresaId, sedeId, placa);
        return ResponseEntity.ok(suscripcionMensualUseCase.listSuscripciones(empresaId, sedeId, placa));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar suscripcion mensual", responses = {
        @ApiResponse(responseCode = "204", description = "Suscripcion cancelada"),
        @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada")
    })
    public ResponseEntity<Void> cancel(
        @PathVariable Long id,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId
    ) {
        log.warn("[SuscripcionMensualController] Cancelar suscripcion id={} empresaId={} sedeId={}", id, empresaId, sedeId);
        suscripcionMensualUseCase.cancelSuscripcion(id, empresaId, sedeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Exportar suscripciones a Excel", responses = {
        @ApiResponse(responseCode = "200", description = "Archivo Excel generado")
    })
    public ResponseEntity<byte[]> exportToExcel(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) String placa
    ) {
        log.info("[SuscripcionMensualController] Exportar a Excel empresaId={} sedeId={} placa={}", empresaId, sedeId, placa);
        try {
            List<SuscripcionMensualResponse> suscripciones = suscripcionMensualUseCase.listSuscripciones(empresaId, sedeId, placa);
            List<SuscripcionMensual> suscripcionesModelo = suscripciones.stream()
                .map(this::toModel)
                .toList();
            
            byte[] excelFile = exportSuscripcionesService.exportToExcel(suscripcionesModelo);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mensualidades.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelFile);
        } catch (Exception e) {
            log.error("[SuscripcionMensualController] Error al exportar a Excel", e);
            throw new RuntimeException("Error al exportar a Excel", e);
        }
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Exportar suscripciones a PDF", responses = {
        @ApiResponse(responseCode = "200", description = "Archivo PDF generado")
    })
    public ResponseEntity<byte[]> exportToPdf(
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long sedeId,
        @RequestParam(required = false) String placa
    ) {
        log.info("[SuscripcionMensualController] Exportar a PDF empresaId={} sedeId={} placa={}", empresaId, sedeId, placa);
        try {
            List<SuscripcionMensualResponse> suscripciones = suscripcionMensualUseCase.listSuscripciones(empresaId, sedeId, placa);
            List<SuscripcionMensual> suscripcionesModelo = suscripciones.stream()
                .map(this::toModel)
                .toList();
            
            byte[] pdfFile = exportSuscripcionesService.exportToPdf(suscripcionesModelo);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mensualidades.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfFile);
        } catch (Exception e) {
            log.error("[SuscripcionMensualController] Error al exportar a PDF", e);
            throw new RuntimeException("Error al exportar a PDF", e);
        }
    }

    private SuscripcionMensual toModel(SuscripcionMensualResponse response) {
        return SuscripcionMensual.builder()
            .id(response.getId())
            .placa(response.getPlaca())
            .tipoVehiculo(response.getTipoVehiculo())
            .valorMensual(response.getValorMensual())
            .fechaInicio(response.getFechaInicio())
            .fechaFin(response.getFechaFin())
            .activa(response.getActiva())
            .sedeId(response.getSedeId())
            .empresaId(response.getEmpresaId())
            .build();
    }
}
