package saas.parqueadero.application.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotUploadResponse {
    private boolean recibido;
    private Instant recibidoEn;
    private long proximaSyncSugeridaSegundos;
}
