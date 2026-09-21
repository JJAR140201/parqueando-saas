package saas.parqueadero.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.ClienteInstalacionResponse;
import saas.parqueadero.application.dto.RegistrarClienteRequest;
import saas.parqueadero.application.dto.RegistrarClienteResponse;
import saas.parqueadero.application.dto.RotarTokenResponse;
import saas.parqueadero.application.dto.SnapshotDetalleResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.model.ResumenOperativo;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.model.SnapshotOperativo;
import saas.parqueadero.domain.port.in.ProveedorMonitoreoUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.ClienteInstalacionRepositoryPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.configuration.sync.SyncProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorMonitoreoService implements ProveedorMonitoreoUseCase {

    /** Ruta relativa del endpoint que consume la instalacion de escritorio. */
    public static final String SNAPSHOT_PATH = "/api/v1/sync/snapshot";

    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final ClienteInstalacionRepositoryPort clienteRepository;
    private final SnapshotOperativoRepositoryPort snapshotRepository;
    private final EmpresaRepositoryPort empresaRepository;
    private final ObjectMapper objectMapper;
    private final SyncProperties syncProperties;

    @Override
    @Transactional
    public RegistrarClienteResponse registrarCliente(RegistrarClienteRequest request) {
        enforceSuperAdmin();

        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new BusinessException("El nombre del cliente es obligatorio");
        }

        Long empresaId = request.getEmpresaId();
        if (empresaId != null) {
            empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
            if (clienteRepository.existsByEmpresaId(empresaId)) {
                throw new BusinessException("Ya existe una instalacion registrada para esa empresa");
            }
        }

        String token = SyncToken.generar();
        ClienteInstalacion creada = clienteRepository.save(ClienteInstalacion.builder()
            .tenantId(UUID.randomUUID().toString())
            .nombre(nombre)
            .empresaId(empresaId)
            .tokenHash(SyncToken.hash(token))
            .activa(true)
            .creadaEn(Instant.now())
            .ultimoResumen(ResumenOperativo.builder().build())
            .build());

        log.info("[ProveedorMonitoreoService] Instalacion registrada tenantId={} nombre={}",
            creada.getTenantId(), nombre);

        return RegistrarClienteResponse.builder()
            .tenantId(creada.getTenantId())
            .token(token)
            .syncUrl(resolverSyncUrl())
            .nombre(creada.getNombre())
            .empresaId(creada.getEmpresaId())
            .creadaEn(creada.getCreadaEn())
            .build();
    }

    @Override
    public List<ClienteInstalacionResponse> listarClientes() {
        enforceSuperAdmin();
        return clienteRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public ClienteInstalacionResponse obtenerCliente(String tenantId) {
        enforceSuperAdmin();
        return toResponse(buscar(tenantId));
    }

    @Override
    public SnapshotDetalleResponse ultimoSnapshot(String tenantId) {
        enforceSuperAdmin();
        ClienteInstalacion cliente = buscar(tenantId);

        SnapshotOperativo snapshot = snapshotRepository.findUltimoByTenantId(cliente.getTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("La instalacion aun no ha sincronizado datos"));

        JsonNode datos;
        try {
            datos = objectMapper.readTree(snapshot.getPayloadJson());
        } catch (Exception ex) {
            throw new BusinessException("El snapshot almacenado esta corrupto");
        }

        ResumenOperativo r = snapshot.getResumen() == null ? ResumenOperativo.builder().build() : snapshot.getResumen();
        return SnapshotDetalleResponse.builder()
            .tenantId(cliente.getTenantId())
            .recibidoEn(snapshot.getRecibidoEn())
            .generadoEn(snapshot.getGeneradoEn())
            .appVersion(snapshot.getAppVersion())
            .estadoLicencia(snapshot.getEstadoLicencia())
            .vehiculosDentro(r.getVehiculosDentro())
            .entradasHoy(r.getEntradasHoy())
            .salidasHoy(r.getSalidasHoy())
            .recaudoHoy(r.getRecaudoHoy())
            .datos(datos)
            .build();
    }

    @Override
    @Transactional
    public RotarTokenResponse rotarToken(String tenantId) {
        enforceSuperAdmin();
        ClienteInstalacion cliente = buscar(tenantId);

        String token = SyncToken.generar();
        cliente.setTokenHash(SyncToken.hash(token));
        clienteRepository.save(cliente);

        log.warn("[ProveedorMonitoreoService] Token rotado tenantId={}", cliente.getTenantId());
        return RotarTokenResponse.builder().tenantId(cliente.getTenantId()).token(token).build();
    }

    @Override
    @Transactional
    public ClienteInstalacionResponse cambiarEstado(String tenantId, boolean activa) {
        enforceSuperAdmin();
        ClienteInstalacion cliente = buscar(tenantId);
        cliente.setActiva(activa);
        return toResponse(clienteRepository.save(cliente));
    }

    // ---- helpers -------------------------------------------------------

    private ClienteInstalacion buscar(String tenantId) {
        return clienteRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Instalacion no encontrada"));
    }

    private String resolverSyncUrl() {
        String base = syncProperties.baseUrl();
        if (base == null || base.isBlank()) {
            return SNAPSHOT_PATH;
        }
        return base.replaceAll("/+$", "") + SNAPSHOT_PATH;
    }

    private ClienteInstalacionResponse toResponse(ClienteInstalacion c) {
        ResumenOperativo r = c.getUltimoResumen() == null ? ResumenOperativo.builder().build() : c.getUltimoResumen();
        return ClienteInstalacionResponse.builder()
            .tenantId(c.getTenantId())
            .nombre(c.getNombre())
            .empresaId(c.getEmpresaId())
            .activa(c.isActiva())
            .creadaEn(c.getCreadaEn())
            .ultimaSyncEn(c.getUltimaSyncEn())
            .appVersion(c.getAppVersion())
            .estadoLicencia(c.getEstadoLicencia())
            .licenciaExpiraEn(c.getLicenciaExpiraEn())
            .vehiculosDentro(r.getVehiculosDentro())
            .entradasHoy(r.getEntradasHoy())
            .salidasHoy(r.getSalidasHoy())
            .recaudoHoy(r.getRecaudoHoy())
            .build();
    }

    private void enforceSuperAdmin() {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        if (currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            throw new BusinessException("No se encontraron roles en el token");
        }
        boolean isSuperAdmin = currentUser.getRoles().stream()
            .map(role -> role.replace("ROLE_", ""))
            .anyMatch(role -> role.equals(RolUsuario.SUPER_ADMIN.name()));
        if (!isSuperAdmin) {
            throw new BusinessException("Solo el SUPER_ADMIN puede administrar instalaciones cliente");
        }
    }
}
