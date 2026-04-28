package saas.parqueadero.infrastructure.configuration.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.exception.BusinessException;
import saas.parqueadero.domain.model.AuthenticatedUser;
import saas.parqueadero.domain.port.out.AuthenticatedUserProviderPort;

@Component
public class SecurityContextAuthenticatedUserProvider implements AuthenticatedUserProviderPort {

    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException("No hay un usuario autenticado en el contexto actual");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new BusinessException("No fue posible resolver los datos del usuario autenticado");
    }
}
