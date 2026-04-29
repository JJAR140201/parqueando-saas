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
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.CreateEmpresaRequest;
import saas.parqueadero.application.dto.CreateEmpresaResponse;
import saas.parqueadero.application.dto.CreateEmpresaUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.application.dto.SedeSummaryResponse;
import saas.parqueadero.application.dto.UpsertTarifasRequest;
import saas.parqueadero.application.dto.UpsertTarifasResponse;
import saas.parqueadero.application.dto.UpdateEmpresaRequest;
import saas.parqueadero.application.dto.UpdateUserRequest;
import saas.parqueadero.domain.port.in.SuperAdminUseCase;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Super Administracion", description = "Gestion global de empresas y sedes")
@SecurityRequirement(name = "bearerAuth")
public class SuperAdminController {

    private final SuperAdminUseCase superAdminUseCase;

    @GetMapping("/empresas/{empresaId}/sedes")
    @Operation(summary = "Listar sedes de una empresa", responses = {
        @ApiResponse(responseCode = "200", description = "Lista de sedes"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<List<SedeSummaryResponse>> listSedesByEmpresa(@PathVariable Long empresaId) {
        log.info("[SuperAdminController] Listar sedes empresaId={}", empresaId);
        return ResponseEntity.ok(superAdminUseCase.listSedesByEmpresa(empresaId));
    }

    @GetMapping("/empresas")
    @Operation(summary = "Listar todas las empresas", responses = {
        @ApiResponse(responseCode = "200", description = "Lista de empresas"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<List<CreateEmpresaResponse>> listEmpresas() {
        log.info("[SuperAdminController] Listar empresas");
        return ResponseEntity.ok(superAdminUseCase.listEmpresas());
    }

    @PostMapping("/empresas")
    @Operation(summary = "Crear empresa con sedes", responses = {
        @ApiResponse(responseCode = "201", description = "Empresa creada"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<CreateEmpresaResponse> createEmpresaWithSedes(@Valid @RequestBody CreateEmpresaRequest request) {
        log.info("[SuperAdminController] Crear empresa nit={} nombre={} sedes={}", request.getNit(), request.getNombre(), request.getSedes().size());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(superAdminUseCase.createEmpresaWithSedes(request));
    }

    @PostMapping("/usuarios")
    @Operation(summary = "Crear usuario por empresa", responses = {
        @ApiResponse(responseCode = "201", description = "Usuario creado"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "404", description = "Empresa o sede no encontrada")
    })
    public ResponseEntity<RegisterUserResponse> createUserForEmpresa(@Valid @RequestBody CreateEmpresaUserRequest request) {
        log.info("[SuperAdminController] Crear usuario por empresa username={} rol={} empresaId={} sedeId={}",
            request.getUsername(), request.getRol(), request.getEmpresaId(), request.getSedeId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(superAdminUseCase.createUserForEmpresa(request));
    }

    @PutMapping("/empresas/{empresaId}")
    @Operation(summary = "Editar empresa con sedes", responses = {
        @ApiResponse(responseCode = "200", description = "Empresa actualizada"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "404", description = "Empresa o sede no encontrada")
    })
    public ResponseEntity<CreateEmpresaResponse> updateEmpresaWithSedes(
        @PathVariable Long empresaId,
        @Valid @RequestBody UpdateEmpresaRequest request
    ) {
        log.info("[SuperAdminController] Actualizar empresa empresaId={} nit={} sedes={}", empresaId, request.getNit(), request.getSedes().size());
        return ResponseEntity.ok(superAdminUseCase.updateEmpresaWithSedes(empresaId, request));
    }

    @PutMapping("/empresas/{empresaId}/sedes/{sedeId}/tarifas")
    @Operation(summary = "Crear o actualizar tarifas de carro y moto por sede", responses = {
        @ApiResponse(responseCode = "200", description = "Tarifas actualizadas"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "404", description = "Empresa o sede no encontrada")
    })
    public ResponseEntity<UpsertTarifasResponse> upsertTarifas(
        @PathVariable Long empresaId,
        @PathVariable Long sedeId,
        @Valid @RequestBody UpsertTarifasRequest request
    ) {
        log.info("[SuperAdminController] Upsert tarifas empresaId={} sedeId={}", empresaId, sedeId);
        return ResponseEntity.ok(superAdminUseCase.upsertTarifas(empresaId, sedeId, request));
    }

    @DeleteMapping("/empresas/{empresaId}")
    @Operation(summary = "Eliminar empresa con sus sedes y usuarios", responses = {
        @ApiResponse(responseCode = "204", description = "Empresa eliminada"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<Void> deleteEmpresa(@PathVariable Long empresaId) {
        log.info("[SuperAdminController] Eliminar empresa empresaId={}", empresaId);
        superAdminUseCase.deleteEmpresa(empresaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/usuarios/{userId}")
    @Operation(summary = "Editar usuario de empresa", responses = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
        @ApiResponse(responseCode = "400", description = "Regla de negocio o validacion"),
        @ApiResponse(responseCode = "404", description = "Usuario o sede no encontrada")
    })
    public ResponseEntity<RegisterUserResponse> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("[SuperAdminController] Actualizar usuario userId={} username={} rol={} sedeId={}",
            userId, request.getUsername(), request.getRol(), request.getSedeId());
        return ResponseEntity.ok(superAdminUseCase.updateUser(userId, request));
    }

    @DeleteMapping("/usuarios/{userId}")
    @Operation(summary = "Eliminar usuario de empresa", responses = {
        @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("[SuperAdminController] Eliminar usuario userId={}", userId);
        superAdminUseCase.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
