package saas.parqueadero.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import saas.parqueadero.domain.port.out.SnapshotOperativoRepositoryPort;
import saas.parqueadero.infrastructure.configuration.sync.SyncProperties;

/** Borra a diario los snapshots historicos mas viejos que {@code app.sync.retencion-dias}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotRetentionService {

    private final SnapshotOperativoRepositoryPort snapshotRepository;
    private final SyncProperties syncProperties;

    @Scheduled(cron = "${app.sync.limpieza-cron:0 30 3 * * *}", zone = "America/Bogota")
    public void purgarAntiguos() {
        Instant limite = Instant.now().minus(syncProperties.retencionDias(), ChronoUnit.DAYS);
        int borrados = snapshotRepository.deleteRecibidosAntesDe(limite);
        if (borrados > 0) {
            log.info("[SnapshotRetentionService] Snapshots purgados: {} (anteriores a {})", borrados, limite);
        }
    }
}
