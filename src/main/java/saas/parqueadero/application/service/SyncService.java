package saas.parqueadero.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.SnapshotUploadRequest;
import saas.parqueadero.application.dto.SnapshotUploadResponse;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.UnauthorizedException;
import saas.parqueadero.domain.model.ClienteInstalacion;
import saas.parqueadero.domain.model.ResumenOperativo;
import saas.parqueadero.domain.model.SnapshotOperativo;
import saas.parqueadero.domain.port.in.SyncUseCase;
import saas.parqueadero.domain.port.out.ClienteInstalacionRepositoryPort;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.configuration.sync.SyncProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService implements SyncUseCase {

    private final ClienteInstalacionRepositoryPort clienteRepository;
    private final SnapshotOperativoRepositoryPort snapshotRepository;
    private final ObjectMapper objectMapper;
    private final SyncProperties syncProperties;

    @Override
    @Transactional
    public SnapshotUploadResponse recibirSnapshot(String tenantId, String token, SnapshotUploadRequest request) {
        if (tenantId == null || tenantId.isBlank() || token == null || token.isBlank()) {
            throw new UnauthorizedException("Faltan credenciales de sincronizacion");
        }

        ClienteInstalacion cliente = clienteRepository.findByTenantId(tenantId.trim())
            .orElseThrow(() -> new UnauthorizedException("Instalacion no registrada"));

        if (!cliente.isActiva()) {
            throw new UnauthorizedException("La instalacion esta desactivada");
        }
        if (!SyncToken.hashesCoinciden(SyncToken.hash(token.trim()), cliente.getTokenHash())) {
            log.warn("[SyncService] Token invalido para tenantId={}", tenantId);
            throw new UnauthorizedException("Token de sincronizacion invalido");
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request.getDatos());
        } catch (Exception ex) {
            throw new BusinessException("El campo 'datos' no es un JSON valido");
        }

        Instant ahora = Instant.now();
        String estadoLicencia = request.getLicencia() == null ? null : request.getLicencia().getEstado();
        ResumenOperativo resumen = mapResumen(request.getResumen());

        snapshotRepository.save(SnapshotOperativo.builder()
            .tenantId(cliente.getTenantId())
            .recibidoEn(ahora)
            .generadoEn(request.getGeneradoEn())
            .appVersion(request.getAppVersion())
            .estadoLicencia(estadoLicencia)
            .resumen(resumen)
            .payloadJson(payloadJson)
            .build());

        cliente.setUltimaSyncEn(ahora);
        cliente.setAppVersion(request.getAppVersion());
        cliente.setEstadoLicencia(estadoLicencia);
        cliente.setLicenciaExpiraEn(request.getLicencia() == null ? null : request.getLicencia().getExpiraEn());
        cliente.setUltimoResumen(resumen);
        clienteRepository.save(cliente);

        log.info("[SyncService] Snapshot recibido tenantId={} version={} generadoEn={}",
            cliente.getTenantId(), request.getAppVersion(), request.getGeneradoEn());

        return SnapshotUploadResponse.builder()
            .recibido(true)
            .recibidoEn(ahora)
            .proximaSyncSugeridaSegundos(syncProperties.intervaloSugeridoSegundos())
            .build();
    }

    private ResumenOperativo mapResumen(SnapshotUploadRequest.Resumen r) {
        if (r == null) {
            return ResumenOperativo.builder().build();
        }
        return ResumenOperativo.builder()
            .vehiculosDentro(r.getVehiculosDentro())
            .entradasHoy(r.getEntradasHoy())
            .salidasHoy(r.getSalidasHoy())
            .recaudoHoy(r.getRecaudoHoy())
            .build();
    }
}
