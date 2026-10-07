package saas.parqueadero.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import saas.parqueadero.domain.exception.TooManyRequestsException;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void bloqueaTrasMaximoDeFallos() {
        for (int i = 0; i < LoginAttemptService.MAX_FALLOS; i++) {
            assertDoesNotThrow(() -> service.verificarNoBloqueado("Admin"));
            service.registrarFallo("admin");
        }
        assertThrows(TooManyRequestsException.class, () -> service.verificarNoBloqueado(" ADMIN "));
    }

    @Test
    void exitoLimpiaElContador() {
        for (int i = 0; i < LoginAttemptService.MAX_FALLOS; i++) {
            service.registrarFallo("operario");
        }
        service.registrarExito("operario");
        assertDoesNotThrow(() -> service.verificarNoBloqueado("operario"));
    }

    @Test
    void otroUsuarioNoSeVeAfectado() {
        for (int i = 0; i < LoginAttemptService.MAX_FALLOS; i++) {
            service.registrarFallo("a");
        }
        assertDoesNotThrow(() -> service.verificarNoBloqueado("b"));
    }
}
