package saas.parqueadero.domain.port.in;

import java.util.List;
import saas.parqueadero.application.dto.CreateSuscripcionMensualRequest;
import saas.parqueadero.application.dto.SuscripcionMensualResponse;
import saas.parqueadero.application.dto.UpdateSuscripcionMensualRequest;

public interface SuscripcionMensualUseCase {
    SuscripcionMensualResponse createSuscripcion(CreateSuscripcionMensualRequest request);

    SuscripcionMensualResponse updateSuscripcion(Long id, UpdateSuscripcionMensualRequest request);

    List<SuscripcionMensualResponse> listSuscripciones(Long empresaId, Long sedeId, String placa);

    void cancelSuscripcion(Long id, Long empresaId, Long sedeId);
}
