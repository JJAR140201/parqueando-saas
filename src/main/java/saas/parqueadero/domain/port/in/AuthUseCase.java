package saas.parqueadero.domain.port.in;

import saas.parqueadero.application.dto.LoginRequest;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.RegisterUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;

public interface AuthUseCase {
    LoginResponse login(LoginRequest request);

    RegisterUserResponse register(RegisterUserRequest request);
}
