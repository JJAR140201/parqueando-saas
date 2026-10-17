package saas.parqueadero.domain.port.in;

import saas.parqueadero.application.dto.BrandingResponse;
import saas.parqueadero.application.dto.UpdateBrandingRequest;

public interface BrandingUseCase {
    /** Marca de la empresa del usuario autenticado (vacia para SUPER_ADMIN, que no pertenece a una empresa). */
    BrandingResponse getMine();

    BrandingResponse update(Long empresaId, UpdateBrandingRequest request);
}
