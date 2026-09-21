package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nuevo token de sincronizacion tras rotarlo. Se muestra una sola vez. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotarTokenResponse {
    private String tenantId;
    private String token;
}
