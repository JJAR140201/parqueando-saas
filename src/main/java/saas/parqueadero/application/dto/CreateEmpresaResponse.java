package saas.parqueadero.application.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmpresaResponse {
    private Long empresaId;
    private String nit;
    private String nombre;
    private List<SedeSummaryResponse> sedes;
}
