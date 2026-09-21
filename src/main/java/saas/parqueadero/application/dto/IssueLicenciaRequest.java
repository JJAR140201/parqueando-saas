package saas.parqueadero.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLicenciaRequest {
    @Min(1)
    private Integer duracionDias;

    @Size(max = 500)
    private String nota;
}
