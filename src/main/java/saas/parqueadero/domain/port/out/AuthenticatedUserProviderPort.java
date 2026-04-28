package saas.parqueadero.domain.port.out;

import saas.parqueadero.domain.model.AuthenticatedUser;

public interface AuthenticatedUserProviderPort {
    AuthenticatedUser getCurrentUser();
}
