package saas.parqueadero.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.exception.TooManyRequestsException;

/**
 * Bloquea temporalmente un username tras varios intentos fallidos de autenticacion
 * (login y renovacion de licencia). Estado en memoria: suficiente para una sola instancia.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);

    private record Intentos(int fallos, Instant primerFallo) {
    }

    private final ConcurrentHashMap<String, Intentos> intentos = new ConcurrentHashMap<>();

    /** Lanza {@link TooManyRequestsException} si el username esta bloqueado. */
    public void verificarNoBloqueado(String username) {
        String clave = clave(username);
        Intentos actual = intentos.get(clave);
        if (actual == null) {
            return;
        }
        if (actual.primerFallo().plus(VENTANA).isBefore(Instant.now())) {
            intentos.remove(clave, actual);
            return;
        }
        if (actual.fallos() >= MAX_FALLOS) {
            throw new TooManyRequestsException("Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
        }
    }

    public void registrarFallo(String username) {
        Instant ahora = Instant.now();
        intentos.merge(clave(username), new Intentos(1, ahora), (previo, nuevo) ->
            previo.primerFallo().plus(VENTANA).isBefore(ahora)
                ? nuevo
                : new Intentos(previo.fallos() + 1, previo.primerFallo()));
        if (intentos.size() > 10_000) {
            intentos.entrySet().removeIf(e -> e.getValue().primerFallo().plus(VENTANA).isBefore(ahora));
        }
    }

    public void registrarExito(String username) {
        intentos.remove(clave(username));
    }

    private String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
