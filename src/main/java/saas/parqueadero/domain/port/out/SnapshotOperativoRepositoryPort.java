package saas.parqueadero.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import saas.parqueadero.domain.model.SnapshotOperativo;

public interface SnapshotOperativoRepositoryPort {

    SnapshotOperativo save(SnapshotOperativo snapshot);

    Optional<SnapshotOperativo> findUltimoByTenantId(String tenantId);

    /** Borra snapshots recibidos antes de la fecha indicada. Devuelve cuantos borro. */
    int deleteRecibidosAntesDe(Instant limite);
}
