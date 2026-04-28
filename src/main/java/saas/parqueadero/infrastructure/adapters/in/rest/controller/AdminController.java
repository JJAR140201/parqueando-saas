package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.UserSummaryResponse;
import saas.parqueadero.domain.port.in.SuperAdminUseCase;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administracion", description = "Gestion administrativa de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final SuperAdminUseCase superAdminUseCase;

    @GetMapping("/usuarios")
    @Operation(summary = "Listar usuarios por empresa", responses = {
        @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
        @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
    })
    public ResponseEntity<List<UserSummaryResponse>> listUsersByEmpresa(@RequestParam Long empresaId) {
        log.info("[AdminController] Listar usuarios empresaId={}", empresaId);
        return ResponseEntity.ok(superAdminUseCase.listUsersByEmpresa(empresaId));
    }
}