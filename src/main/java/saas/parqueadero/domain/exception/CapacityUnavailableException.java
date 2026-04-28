package saas.parqueadero.domain.exception;

public class CapacityUnavailableException extends BusinessException {
    public CapacityUnavailableException(String message) {
        super(message);
    }
}
