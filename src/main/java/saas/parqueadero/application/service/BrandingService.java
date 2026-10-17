package saas.parqueadero.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.parqueadero.application.dto.BrandingResponse;
import saas.parqueadero.application.dto.UpdateBrandingRequest;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.exception.ResourceNotFoundException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.Empresa;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.port.in.BrandingUseCase;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;
import saas.parqueadero.domain.port.out.EmpresaRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandingService implements BrandingUseCase {

    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;
    private final EmpresaRepositoryPort empresaRepositoryPort;

    @Override
    public BrandingResponse getMine() {
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        if (currentUser.getEmpresaId() == null) {
            return BrandingResponse.builder().build();
        }
        return empresaRepositoryPort.findById(currentUser.getEmpresaId())
            .map(this::toResponse)
            .orElseGet(() -> BrandingResponse.builder().build());
    }

    @Override
    @Transactional
    public BrandingResponse update(Long empresaId, UpdateBrandingRequest request) {
        log.info("[BrandingService] Actualizar marca empresaId={}", empresaId);
        AuthenticatedUser currentUser = authenticatedUserProviderPort.getCurrentUser();
        boolean superAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
            .map(rol -> rol.replace("ROLE_", ""))
            .anyMatch(rol -> rol.equals(RolUsuario.SUPER_ADMIN.name()));
        if (!superAdmin) {
            throw new BusinessException("Solo SUPER_ADMIN puede configurar la marca de una empresa");
        }

        Empresa empresa = empresaRepositoryPort.findById(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        empresa.setColorMarca(blankToNull(request.getColorMarca()));
        empresa.setLogo(blankToNull(request.getLogo()));
        return toResponse(empresaRepositoryPort.save(empresa));
    }

    private BrandingResponse toResponse(Empresa empresa) {
        return BrandingResponse.builder()
            .empresaId(empresa.getId())
            .nombre(empresa.getNombre())
            .colorMarca(empresa.getColorMarca())
            .logo(empresa.getLogo())
            .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
