package saas.parqueadero.application.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import saas.parqueadero.application.dto.PrecioSalidaResponse;

/**
 * Servicio para enviar mensajes SMS a través de Twilio.
 * Se utiliza para enviar el recibo de parqueo por SMS.
 */
@Service
@Slf4j
public class TwilioService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    private boolean isConfigured = false;

    /**
     * Inicializa Twilio después de la inyección de dependencias.
     * Se ejecuta solo una vez al cargar el servicio.
     */
    @PostConstruct
    public void initTwilio() {
        // Validar que las credenciales estén configuradas
        if (accountSid == null || accountSid.trim().isEmpty() ||
            authToken == null || authToken.trim().isEmpty() ||
            twilioPhoneNumber == null || twilioPhoneNumber.trim().isEmpty()) {
            log.warn("[TwilioService] Credenciales de Twilio no configuradas. Las credenciales deben estar en variables de entorno o application.properties");
            isConfigured = false;
            return;
        }

        try {
            // Inicializar Twilio con credenciales (solo una vez)
            Twilio.init(accountSid, authToken);
            isConfigured = true;
            log.info("[TwilioService] Twilio inicializado exitosamente. Número: {}", twilioPhoneNumber);
        } catch (Exception e) {
            log.error("[TwilioService] Error inicializando Twilio", e);
            isConfigured = false;
        }
    }

    /**
     * Envía un recibo de parqueo por SMS.
     *
     * @param phoneNumber número de teléfono del cliente (formato E.164)
     * @param precio datos del recibo de parqueo
     * @return true si el mensaje fue enviado correctamente
     */
    public boolean enviarReciboPorSMS(String phoneNumber, PrecioSalidaResponse precio) {
        boolean enviado = enviarSms(phoneNumber, generarMensajeRecibo(precio));
        if (enviado) {
            log.info("[TwilioService] Recibo SMS enviado exitosamente placa={}", precio.getPlaca());
        }
        return enviado;
    }

    /**
     * Envía un mensaje de texto arbitrario por SMS.
     *
     * @param phoneNumber número de teléfono del destinatario (formato E.164)
     * @param mensaje contenido del mensaje
     * @return true si el mensaje fue enviado correctamente
     */
    public boolean enviarSms(String phoneNumber, String mensaje) {
        if (!isConfigured) {
            log.error("[TwilioService] No se puede enviar SMS: Twilio no está configurado. Verifique TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_PHONE_NUMBER");
            return false;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),        // To number
                    new PhoneNumber(twilioPhoneNumber),  // From number
                    mensaje                               // Message content
            ).create();

            log.info("[TwilioService] SMS enviado exitosamente. MessageSid={}", message.getSid());
            return true;

        } catch (Exception e) {
            log.error("[TwilioService] Error enviando SMS a {}", phoneNumber, e);
            return false;
        }
    }

    /**
     * Genera el contenido del mensaje SMS con los datos del recibo.
     * Optimizado para SMS (menos de 160 caracteres o múltiples SMS de 160).
     */
    private String generarMensajeRecibo(PrecioSalidaResponse precio) {
        StringBuilder mensaje = new StringBuilder();

        mensaje.append(precio.getNombreEmpresa()).append("\n");
        mensaje.append("===== RECIBO DE PARQUEO =====\n");

        // Información del vehículo
        if (precio.getPlaca() != null) {
            mensaje.append("Placa: ").append(precio.getPlaca()).append("\n");
        }
        if (precio.getTipoVehiculo() != null) {
            mensaje.append("Tipo: ").append(precio.getTipoVehiculo().name()).append("\n");
        }

        // Fechas y horarios
        if (precio.getFechaEntrada() != null) {
            mensaje.append("Entrada: ").append(precio.getFechaEntrada().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )).append("\n");
        }
        if (precio.getFechaSalida() != null) {
            mensaje.append("Salida: ").append(precio.getFechaSalida().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )).append("\n");
        }

        // Duración
        if (precio.getHoras() != null) {
            mensaje.append("Duración: ").append(precio.getHoras()).append(" horas\n");
        }

        // Total pagado
        mensaje.append("=============================\n");
        if (Boolean.TRUE.equals(precio.getMensualidadActiva())) {
            mensaje.append("TOTAL: SIN COSTO (Mensualidad)\n");
        } else {
            String total = precio.getTotalPagado() != null
                    ? "$ " + precio.getTotalPagado()
                    : (precio.getTotal() != null ? "$ " + precio.getTotal() : "$ 0.00");
            mensaje.append("TOTAL: ").append(total).append("\n");
        }

        mensaje.append("=============================\n");
        mensaje.append("GRACIAS POR SU VISITA\n");
        mensaje.append("MANEJE CON PRECAUCION");

        return mensaje.toString();
    }

    /**
     * Valida que un número de teléfono esté en formato válido (E.164).
     * Formato: +<país><número> (ej: +573001234567)
     */
    public boolean esNumeroValido(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // Formato E.164: + seguido de 1-15 dígitos
        return phoneNumber.matches("^\\+[1-9]\\d{1,14}$");
    }
}
