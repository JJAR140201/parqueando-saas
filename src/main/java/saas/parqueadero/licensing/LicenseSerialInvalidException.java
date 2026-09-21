package saas.parqueadero.licensing;

/** El serial no tiene el formato esperado o la firma no coincide. */
public class LicenseSerialInvalidException extends RuntimeException {

    public LicenseSerialInvalidException(String message) {
        super(message);
    }
}
