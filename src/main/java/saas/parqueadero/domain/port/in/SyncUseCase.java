package saas.parqueadero.domain.port.in;

import saas.parqueadero.application.dto.SnapshotUploadRequest;
import saas.parqueadero.application.dto.SnapshotUploadResponse;

/**
 * Recepcion de snapshots enviados por las instalaciones de escritorio.
 * La autenticacion es por {@code tenantId} + token de sincronizacion (no JWT).
 */
public interface SyncUseCase {

    SnapshotUploadResponse recibirSnapshot(String tenantId, String token, SnapshotUploadRequest request);
}
