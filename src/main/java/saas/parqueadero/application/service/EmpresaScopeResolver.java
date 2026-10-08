package saas.parqueadero.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.model.RolUsuario;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;

/**
 * Determina sobre que empresa puede operar el usuario actual. Es la barrera multiempresa de WhatsApp:
 * un ADMIN u OPERARIO solo ve la suya; un SUPER_ADMIN debe indicarla.
 */
@Component
@RequiredArgsConstructor
public class EmpresaScopeResolver {

    private final AuthenticatedUserProviderPort authenticatedUserProviderPort;

    /**
     * @param empresaIdSolicitada empresa pedida en la peticion (solo la usa el SUPER_ADMIN)
     * @param permitirOperario    si el rol OPERARIO tambien puede operar
     */
    public Long resolver(Long empresaIdSolicitada, boolean permitirOperario) {
        AuthenticatedUser usuario = authenticatedUserProviderPort.getCurrentUser();

        if (tieneRol(usuario, RolUsuario.SUPER_ADMIN)) {
            if (empresaIdSolicitada == null) {
                throw new BusinessException("SUPER_ADMIN debe indicar empresaId");
            }
            return empresaIdSolicitada;
        }

        boolean permitido = tieneRol(usuario, RolUsuario.ADMIN) || (permitirOperario && tieneRol(usuario, RolUsuario.OPERARIO));
        if (!permitido) {
            throw new BusinessException("Tu rol no puede realizar esta operacion");
        }
        if (usuario.getEmpresaId() == null) {
            throw new BusinessException("El token no contiene empresaId");
        }
        if (empresaIdSolicitada != null && !empresaIdSolicitada.equals(usuario.getEmpresaId())) {
            throw new BusinessException("No puedes operar sobre otra empresa");
        }
        return usuario.getEmpresaId();
    }

    private boolean tieneRol(AuthenticatedUser usuario, RolUsuario rol) {
        return usuario.getRoles() != null && usuario.getRoles().stream()
            .map(r -> r.replace("ROLE_", ""))
            .anyMatch(r -> r.equals(rol.name()));
    }
}
