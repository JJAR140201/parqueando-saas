package saas.parqueadero.application.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertTarifasResponse {
    private Long empresaId;
    private Long sedeId;

    private BigDecimal valorFraccionCarro;
    private Integer minutosFraccionCarro;

    private BigDecimal valorFraccionMoto;
    private Integer minutosFraccionMoto;
}
