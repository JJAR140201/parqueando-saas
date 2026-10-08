package saas.parqueadero.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.model.Cliente;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.SuscripcionMensual;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappAccountStatus;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.in.MensualidadNotificacionUseCase;
import saas.parqueadero.domain.port.out.ClienteRepositoryPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SuscripcionMensualRepositoryPort;
import saas.parqueadero.domain.port.out.TokenCipherPort;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappGateway;
import saas.parqueadero.domain.port.out.WhatsappGateway.Credenciales;
import saas.parqueadero.domain.port.out.WhatsappGateway.EnvioResultado;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;
import saas.parqueadero.infrastructure.configuration.whatsapp.WhatsappProperties;

/**
 * Decide si se notifica a un cliente, arma el mensaje, lo envia por el {@link WhatsappGateway} y deja
 * el registro. Es independiente de la operacion de negocio que lo origino: nada de lo que ocurra aqui
 * (Meta caido, token vencido, telefono invalido) se propaga hacia ella.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappNotificationService implements MensualidadNotificacionUseCase {

    private static final Pattern TELEFONO_E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    /** Codigos de Meta que indican un problema de credenciales de la cuenta. */
    private static final Set<String> ERRORES_DE_CREDENCIALES = Set.of("META_190", "META_10", "META_200", "META_102");

    private final SuscripcionMensualRepositoryPort suscripcionRepository;
    private final EmpresaRepositoryPort empresaRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final WhatsappAccountRepositoryPort accountRepository;
    private final WhatsappMessageRepositoryPort messageRepository;
    private final WhatsappGateway gateway;
    private final TokenCipherPort tokenCipher;
    private final WhatsappMensajeFormatter formatter;
    private final WhatsappProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public Resultado notificar(TipoNotificacionWhatsapp tipo, Long suscripcionId) {
        try {
            return notificarInterno(tipo, suscripcionId);
        } catch (Exception ex) {
            log.error("[WhatsappNotificationService] Error inesperado notificando {} de la mensualidad {}",
                tipo, suscripcionId, ex);
            return Resultado.FALLIDA;
        }
    }

    private Resultado notificarInterno(TipoNotificacionWhatsapp tipo, Long suscripcionId) {
        SuscripcionMensual suscripcion = suscripcionRepository.findById(suscripcionId).orElse(null);
        if (suscripcion == null) {
            log.warn("[WhatsappNotificationService] Mensualidad {} no encontrada: no se notifica", suscripcionId);
            return Resultado.FALLIDA;
        }
        Empresa empresa = empresaRepository.findById(suscripcion.getEmpresaId()).orElse(null);
        if (empresa == null) {
            log.warn("[WhatsappNotificationService] Empresa {} no encontrada: no se notifica", suscripcion.getEmpresaId());
            return Resultado.FALLIDA;
        }

        // Solo la cuenta de la empresa duena de la mensualidad: una empresa jamas usa la de otra
        WhatsappAccount cuenta = accountRepository.findByEmpresaId(empresa.getId())
            .filter(WhatsappAccount::isEnabled)
            .orElse(null);
        if (cuenta == null) {
            log.debug("[WhatsappNotificationService] Empresa {} sin cuenta de WhatsApp habilitada", empresa.getId());
            return Resultado.OMITIDA_SIN_CUENTA;
        }

        Cliente cliente = suscripcion.getClienteId() == null ? null
            : clienteRepository.findById(suscripcion.getClienteId())
                .filter(c -> empresa.getId().equals(c.getEmpresaId()))
                .orElse(null);
        String destino = cliente != null ? cliente.getTelefono() : String.valueOf(suscripcion.getTelefono());

        if (cliente == null) {
            registrarNoEnviado(cuenta, null, suscripcion, destino, tipo, "SIN_CLIENTE", "La mensualidad no tiene un cliente asociado");
            return Resultado.OMITIDA_NO_PERMITIDA;
        }
        if (!permitido(cliente, tipo)) {
            registrarNoEnviado(cuenta, cliente, suscripcion, destino, tipo, "PREFERENCIA_DESHABILITADA",
                "El cliente no acepta este tipo de mensajes por WhatsApp");
            return Resultado.OMITIDA_NO_PERMITIDA;
        }
        if (!telefonoValido(cliente.getTelefono())) {
            registrarNoEnviado(cuenta, cliente, suscripcion, destino, tipo, "TELEFONO_INVALIDO",
                "El telefono del cliente no tiene formato internacional valido");
            return Resultado.OMITIDA_TELEFONO_INVALIDO;
        }

        List<String> variables = formatter.variables(tipo, cliente, suscripcion, empresa, java.time.LocalDate.now(clock));
        WhatsappMessage mensaje = enviar(cuenta, cliente.getId(), suscripcion.getId(), cliente.getTelefono(), tipo, variables);
        return mensaje.getStatus() == WhatsappMessageStatus.ACCEPTED ? Resultado.ENVIADA : Resultado.FALLIDA;
    }

    /** Envia un mensaje de plantilla de ejemplo para validar la cuenta (no depende de una mensualidad). */
    public WhatsappMessage enviarPrueba(WhatsappAccount cuenta, String nombreEmpresa, String telefono) {
        List<String> variables = formatter.variablesDePrueba(nombreEmpresa, java.time.LocalDate.now(clock));
        if (!telefonoValido(telefono)) {
            return registrarNoEnviado(cuenta, null, null, telefono, TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA,
                "TELEFONO_INVALIDO", "El telefono no tiene formato internacional valido");
        }
        return enviar(cuenta, null, null, telefono, TipoNotificacionWhatsapp.MENSUALIDAD_GENERADA, variables);
    }

    public static boolean telefonoValido(String telefono) {
        return telefono != null && TELEFONO_E164.matcher(telefono).matches();
    }

    static boolean permitido(Cliente cliente, TipoNotificacionWhatsapp tipo) {
        if (!cliente.isWhatsappHabilitado()) {
            return false;
        }
        return switch (tipo) {
            case MENSUALIDAD_GENERADA -> cliente.isPrefFacturacion();
            case RECORDATORIO_VENCIMIENTO, MENSUALIDAD_VENCIDA -> cliente.isPrefMensualidades();
            case CONFIRMACION_PAGO -> cliente.isPrefConfirmacionesPago();
        };
    }

    private WhatsappMessage enviar(WhatsappAccount cuenta, Long clienteId, Long suscripcionId, String destino,
        TipoNotificacionWhatsapp tipo, List<String> variables) {
        Instant solicitado = clock.instant();
        WhatsappMessage mensaje = messageRepository.save(WhatsappMessage.builder()
            .empresaId(cuenta.getEmpresaId())
            .whatsappAccountId(cuenta.getId())
            .clienteId(clienteId)
            .suscripcionId(suscripcionId)
            .destination(destino)
            .messageType(tipo)
            .templateName(tipo.plantilla())
            .templateVariables(aJson(variables))
            .status(WhatsappMessageStatus.REQUESTED)
            .requestedAt(solicitado)
            .createdAt(solicitado)
            .build());

        EnvioResultado resultado;
        try {
            Credenciales credenciales = new Credenciales(cuenta.getPhoneNumberId(),
                tokenCipher.descifrar(cuenta.getAccessTokenEncrypted()));
            resultado = gateway.enviarPlantilla(credenciales, destino, tipo.plantilla(), properties.templateLanguage(), variables);
        } catch (Exception ex) {
            resultado = EnvioResultado.fallido("INTERNAL_ERROR", ex.getClass().getSimpleName());
        }

        Instant respuesta = clock.instant();
        if (resultado.aceptado()) {
            mensaje.setStatus(WhatsappMessageStatus.ACCEPTED);
            mensaje.setProviderMessageId(resultado.providerMessageId());
            mensaje.setProviderAcceptedAt(respuesta);
        } else {
            mensaje.setStatus(WhatsappMessageStatus.FAILED);
            mensaje.setFailedAt(respuesta);
            mensaje.setErrorCode(resultado.errorCode());
            mensaje.setErrorMessage(resultado.errorMessage());
        }
        WhatsappMessage guardado = messageRepository.save(mensaje);
        actualizarEstadoDeCuenta(cuenta, resultado);

        log.info("[WhatsappNotificationService] Mensaje {} tipo={} empresaId={} estado={}",
            guardado.getId(), tipo, cuenta.getEmpresaId(), guardado.getStatus());
        return guardado;
    }

    private WhatsappMessage registrarNoEnviado(WhatsappAccount cuenta, Cliente cliente, SuscripcionMensual suscripcion,
        String destino, TipoNotificacionWhatsapp tipo, String codigo, String descripcion) {
        Instant ahora = clock.instant();
        return messageRepository.save(WhatsappMessage.builder()
            .empresaId(cuenta.getEmpresaId())
            .whatsappAccountId(cuenta.getId())
            .clienteId(cliente == null ? null : cliente.getId())
            .suscripcionId(suscripcion == null ? null : suscripcion.getId())
            .destination(destino == null ? "-" : destino)
            .messageType(tipo)
            .templateName(tipo.plantilla())
            .status(WhatsappMessageStatus.NOT_SENT)
            .requestedAt(ahora)
            .createdAt(ahora)
            .errorCode(codigo)
            .errorMessage(descripcion)
            .build());
    }

    private void actualizarEstadoDeCuenta(WhatsappAccount cuenta, EnvioResultado resultado) {
        try {
            WhatsappAccountStatus nuevo = null;
            if (resultado.aceptado() && cuenta.getStatus() != WhatsappAccountStatus.ACTIVA) {
                nuevo = WhatsappAccountStatus.ACTIVA;
            } else if (!resultado.aceptado() && ERRORES_DE_CREDENCIALES.contains(resultado.errorCode())
                && cuenta.getStatus() != WhatsappAccountStatus.ERROR) {
                nuevo = WhatsappAccountStatus.ERROR;
            }
            if (nuevo != null) {
                cuenta.setStatus(nuevo);
                cuenta.setUpdatedAt(clock.instant());
                accountRepository.save(cuenta);
            }
        } catch (Exception ex) {
            log.warn("[WhatsappNotificationService] No se pudo actualizar el estado de la cuenta {}", cuenta.getId());
        }
    }

    private String aJson(List<String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
