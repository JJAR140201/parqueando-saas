package saas.parqueadero.infrastructure.adapters.in.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.parqueadero.application.dto.ClienteResponse;
import saas.parqueadero.application.dto.UpdateClienteRequest;
import saas.parqueadero.domain.port.in.ClienteUseCase;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Clientes de la empresa y sus preferencias de notificacion")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteUseCase clienteUseCase;

    @GetMapping
    @Operation(summary = "Listar los clientes de la empresa")
    public ResponseEntity<List<ClienteResponse>> listar(@RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(clienteUseCase.listar(empresaId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre, consentimiento y preferencias de un cliente")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UpdateClienteRequest request) {
        return ResponseEntity.ok(clienteUseCase.actualizar(id, request));
    }
}
