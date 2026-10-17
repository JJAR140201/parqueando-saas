package saas.parqueadero.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingResponse {
    private Long empresaId;
    private String nombre;
    private String colorMarca;
    private String logo;
}
