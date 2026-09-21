package saas.parqueadero.domain.port.in;

import java.util.List;
import saas.parqueadero.application.dto.IssueLicenciaRequest;
import saas.parqueadero.application.dto.LicenciaIssuedResponse;
import saas.parqueadero.application.dto.LicenciaRedemptionRequest;
import saas.parqueadero.application.dto.LicenciaSummaryResponse;
import saas.parqueadero.application.dto.LicenciaValidationResponse;
import saas.parqueadero.application.dto.LoginResponse;
import saas.parqueadero.application.dto.ValidateLicenciaRequest;

public interface LicenciaUseCase {
    LicenciaIssuedResponse issue(IssueLicenciaRequest request);

    List<LicenciaSummaryResponse> list();

    LicenciaSummaryResponse revoke(Long licenciaId);

    LicenciaValidationResponse validate(ValidateLicenciaRequest request);

    LoginResponse redeem(LicenciaRedemptionRequest request);
}
