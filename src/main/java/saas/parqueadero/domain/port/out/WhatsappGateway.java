package saas.parqueadero.domain.port.out;

import java.util.List;

/**
 * Salida hacia un proveedor de WhatsApp (hoy Meta Cloud API). No lanza excepciones: los fallos se
 * devuelven en {@link EnvioResultado} para que un problema de WhatsApp nunca afecte al proceso de negocio.
 */
public interface WhatsappGateway {

    EnvioResultado enviarPlantilla(Credenciales credenciales, String destino, String plantilla, String idioma,
        List<String> variables);

    /** Credenciales ya descifradas; el token no aparece en toString para no filtrarse a logs. */
    record Credenciales(String phoneNumberId, String accessToken) {
        @Override
        public String toString() {
            return "Credenciales[phoneNumberId=" + phoneNumberId + ", accessToken=***]";
        }
    }

    record EnvioResultado(boolean aceptado, String providerMessageId, String errorCode, String errorMessage) {
        public static EnvioResultado aceptado(String providerMessageId) {
            return new EnvioResultado(true, providerMessageId, null, null);
        }

        public static EnvioResultado fallido(String errorCode, String errorMessage) {
            return new EnvioResultado(false, null, errorCode, errorMessage);
        }
    }
}
