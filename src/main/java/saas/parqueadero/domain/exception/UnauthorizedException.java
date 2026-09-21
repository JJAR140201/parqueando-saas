package saas.parqueadero.domain.exception;

/** Credenciales ausentes o invalidas (se traduce a HTTP 401). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
