package saas.parqueadero.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.UpsertWhatsappCuentaRequest;
import saas.parqueadero.application.dto.WhatsappCuentaResponse;
import saas.parqueadero.application.dto.WhatsappMensajeResponse;
import saas.parqueadero.application.dto.WhatsappMensajesPaginaResponse;
import saas.parqueadero.application.dto.WhatsappMetricasResponse;
import saas.parqueadero.application.dto.WhatsappMetricasResponse.Latencia;
import saas.parqueadero.application.dto.WhatsappPruebaRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.Pagina;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.model.WhatsappAccountStatus;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.in.WhatsappAdminUseCase;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.TokenCipherPort;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;

@Service
@RequiredArgsConstructor
public class WhatsappAdminService implements WhatsappAdminUseCase {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int MAX_MUESTRAS_METRICAS = 5000;
    private static final String PROVEEDOR = "META";

    private final WhatsappAccountRepositoryPort accountRepository;
    private final WhatsappMessageRepositoryPort messageRepository;
    private final EmpresaRepositoryPort empresaRepository;
    private final TokenCipherPort tokenCipher;
    private final EmpresaScopeResolver scopeResolver;
    private final WhatsappNotificationService notificationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public WhatsappCuentaResponse obtenerCuenta(Long empresaId) {
        Long empresa = scopeResolver.resolver(empresaId, false);
        return accountRepository.findByEmpresaId(empresa).map(WhatsappAdminService::toResponse).orElse(null);
    }

    @Override
    @Transactional
    public WhatsappCuentaResponse guardarCuenta(Long empresaId, UpsertWhatsappCuentaRequest request) {
        Long empresa = scopeResolver.resolver(empresaId, false);
        empresaRepository.findById(empresa).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        String phoneNumberId = request.getPhoneNumberId().trim();
        accountRepository.findByPhoneNumberId(phoneNumberId)
            .filter(otra -> !otra.getEmpresaId().equals(empresa))
            .ifPresent(otra -> {
                throw new BusinessException("Ese Phone Number ID no esta disponible");
            });

        WhatsappAccount existente = accountRepository.findByEmpresaId(empresa).orElse(null);
        boolean traeToken = request.getAccessToken() != null && !request.getAccessToken().isBlank();
        if (existente == null && !traeToken) {
            throw new BusinessException("El token de acceso es obligatorio al crear la configuracion");
        }
        boolean habilitar = Boolean.TRUE.equals(request.getEnabled());
        if ((habilitar || traeToken) && !tokenCipher.estaConfigurado()) {
            throw new BusinessException(
                "El cifrado de WhatsApp no esta configurado en el servidor (WHATSAPP_ENCRYPTION_KEY)");
        }

        Instant ahora = clock.instant();
        WhatsappAccount cuenta = existente != null ? existente : WhatsappAccount.builder()
            .empresaId(empresa)
            .provider(PROVEEDOR)
            .createdAt(ahora)
            .status(WhatsappAccountStatus.PENDIENTE)
            .build();

        boolean cambioCredenciales = existente == null || traeToken || !phoneNumberId.equals(existente.getPhoneNumberId());
        cuenta.setWabaId(request.getWabaId().trim());
        cuenta.setBusinessId(request.getBusinessId() == null || request.getBusinessId().isBlank() ? null : request.getBusinessId().trim());
        cuenta.setPhoneNumberId(phoneNumberId);
        cuenta.setDisplayPhoneNumber(request.getDisplayPhoneNumber().trim());
        if (traeToken) {
            cuenta.setAccessTokenEncrypted(tokenCipher.cifrar(request.getAccessToken().trim()));
        }
        if (cambioCredenciales) {
            // Hasta que un envio real lo confirme, la cuenta vuelve a estar pendiente
            cuenta.setStatus(WhatsappAccountStatus.PENDIENTE);
        }
        cuenta.setEnabled(habilitar);
        cuenta.setUpdatedAt(ahora);
        return toResponse(accountRepository.save(cuenta));
    }

    @Override
    @Transactional
    public WhatsappMensajeResponse enviarPrueba(Long empresaId, WhatsappPruebaRequest request) {
        Long empresa = scopeResolver.resolver(empresaId, false);
        WhatsappAccount cuenta = accountRepository.findByEmpresaId(empresa)
            .orElseThrow(() -> new BusinessException("Primero configura la cuenta de WhatsApp de la empresa"));
        if (!tokenCipher.estaConfigurado()) {
            throw new BusinessException("El cifrado de WhatsApp no esta configurado en el servidor");
        }
        Empresa datosEmpresa = empresaRepository.findById(empresa)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return toResponse(notificationService.enviarPrueba(cuenta, datosEmpresa.getNombre(), request.getTelefono()));
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappMensajesPaginaResponse listarMensajes(Long empresaId, WhatsappMessageStatus estado, TipoNotificacionWhatsapp tipo,
        LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        if (pagina < 0) {
            throw new BusinessException("La pagina no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO_PAGINA) {
            throw new BusinessException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA);
        }
        Long empresa = scopeResolver.resolver(empresaId, false);
        Pagina<WhatsappMessage> resultado = messageRepository.findPagina(
            empresa, estado, tipo, inicioDelDia(desde), finDelDia(hasta), pagina, tamano);
        return new WhatsappMensajesPaginaResponse(
            resultado.contenido().stream().map(WhatsappAdminService::toResponse).toList(), resultado.total());
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsappMetricasResponse metricas(Long empresaId, LocalDate desde, LocalDate hasta) {
        Long empresa = scopeResolver.resolver(empresaId, false);
        List<WhatsappMessage> mensajes = messageRepository.findParaMetricas(
            empresa, inicioDelDia(desde), finDelDia(hasta), MAX_MUESTRAS_METRICAS);

        Map<WhatsappMessageStatus, Long> conteo = new EnumMap<>(WhatsappMessageStatus.class);
        List<Long> api = new ArrayList<>();
        List<Long> envio = new ArrayList<>();
        List<Long> entrega = new ArrayList<>();
        for (WhatsappMessage m : mensajes) {
            conteo.merge(m.getStatus(), 1L, Long::sum);
            agregar(api, m.getRequestedAt(), m.getProviderAcceptedAt());
            agregar(envio, m.getRequestedAt(), m.getSentAt());
            agregar(entrega, m.getRequestedAt(), m.getDeliveredAt());
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (WhatsappMessageStatus estado : WhatsappMessageStatus.values()) {
            porEstado.put(estado.name(), conteo.getOrDefault(estado, 0L));
        }
        return new WhatsappMetricasResponse(mensajes.size(), porEstado, latencia(api), latencia(envio), latencia(entrega));
    }

    private static void agregar(List<Long> destino, Instant desde, Instant hasta) {
        if (desde != null && hasta != null) {
            destino.add(Math.max(0, hasta.toEpochMilli() - desde.toEpochMilli()));
        }
    }

    static Latencia latencia(List<Long> valores) {
        if (valores.isEmpty()) {
            return new Latencia(0, null, null, null);
        }
        List<Long> ordenados = valores.stream().sorted().toList();
        long promedio = Math.round(ordenados.stream().mapToLong(Long::longValue).average().orElse(0));
        return new Latencia(ordenados.size(), promedio, percentil(ordenados, 50), percentil(ordenados, 95));
    }

    /** Percentil por rango mas cercano sobre una lista ya ordenada. */
    static long percentil(List<Long> ordenados, int p) {
        int indice = (int) Math.ceil(p / 100.0 * ordenados.size()) - 1;
        return ordenados.get(Math.max(0, Math.min(indice, ordenados.size() - 1)));
    }

    private static Instant inicioDelDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay(ZONA).toInstant();
    }

    private static Instant finDelDia(LocalDate fecha) {
        return fecha == null ? null : fecha.plusDays(1).atStartOfDay(ZONA).toInstant().minusMillis(1);
    }

    private static WhatsappCuentaResponse toResponse(WhatsappAccount cuenta) {
        return WhatsappCuentaResponse.builder()
            .id(cuenta.getId())
            .empresaId(cuenta.getEmpresaId())
            .provider(cuenta.getProvider())
            .wabaId(cuenta.getWabaId())
            .businessId(cuenta.getBusinessId())
            .phoneNumberId(cuenta.getPhoneNumberId())
            .displayPhoneNumber(cuenta.getDisplayPhoneNumber())
            .status(cuenta.getStatus().name())
            .enabled(cuenta.isEnabled())
            .tokenConfigurado(cuenta.getAccessTokenEncrypted() != null && !cuenta.getAccessTokenEncrypted().isBlank())
            .createdAt(cuenta.getCreatedAt())
            .updatedAt(cuenta.getUpdatedAt())
            .build();
    }

    static WhatsappMensajeResponse toResponse(WhatsappMessage m) {
        return WhatsappMensajeResponse.builder()
            .id(m.getId())
            .empresaId(m.getEmpresaId())
            .clienteId(m.getClienteId())
            .suscripcionId(m.getSuscripcionId())
            .destination(m.getDestination())
            .messageType(m.getMessageType().name())
            .templateName(m.getTemplateName())
            .providerMessageId(m.getProviderMessageId())
            .status(m.getStatus().name())
            .requestedAt(m.getRequestedAt())
            .providerAcceptedAt(m.getProviderAcceptedAt())
            .sentAt(m.getSentAt())
            .deliveredAt(m.getDeliveredAt())
            .readAt(m.getReadAt())
            .failedAt(m.getFailedAt())
            .errorCode(m.getErrorCode())
            .errorMessage(m.getErrorMessage())
            .latenciaApiMs(milis(m.getRequestedAt(), m.getProviderAcceptedAt()))
            .latenciaEnvioMs(milis(m.getRequestedAt(), m.getSentAt()))
            .latenciaEntregaMs(milis(m.getRequestedAt(), m.getDeliveredAt()))
            .build();
    }

    private static Long milis(Instant desde, Instant hasta) {
        return desde == null || hasta == null ? null : Math.max(0, hasta.toEpochMilli() - desde.toEpochMilli());
    }
}
