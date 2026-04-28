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
import saas.parqueadero.application.dto.LoginRequest;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.RegisterUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;
import saas.parqueadero.domain.port.in.AuthUseCase;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticacion", description = "Login y registro de usuarios")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", responses = {
        @ApiResponse(responseCode = "200", description = "Login exitoso"),
        @ApiResponse(responseCode = "400", description = "Credenciales invalidas"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AuthController] Intento de login para username={}", request.getUsername());
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", responses = {
        @ApiResponse(responseCode = "201", description = "Usuario creado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos o regla de negocio"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "404", description = "Sede no encontrada")
    })
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        log.info("[AuthController] Solicitud de registro username={} rol={} empresaId={} sedeId={}",
            request.getUsername(), request.getRol(), request.getEmpresaId(), request.getSedeId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authUseCase.register(request));
    }
}
