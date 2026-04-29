package saas.parqueadero.domain.port.in;

import java.util.List;
import saas.parqueadero.application.dto.CreateEmpresaRequest;
import saas.parqueadero.application.dto.CreateEmpresaResponse;
import saas.parqueadero.application.dto.CreateEmpresaUserRequest;
import saas.parqueadero.application.dto.SedeSummaryResponse;
import saas.parqueadero.application.dto.UpsertTarifasRequest;
import saas.parqueadero.application.dto.UpsertTarifasResponse;
import saas.parqueadero.application.dto.UserSummaryResponse;
import saas.parqueadero.application.dto.UpdateEmpresaRequest;
import saas.parqueadero.application.dto.UpdateUserRequest;
import saas.parqueadero.application.dto.RegisterUserResponse;

public interface SuperAdminUseCase {
    List<CreateEmpresaResponse> listEmpresas();

    List<SedeSummaryResponse> listSedesByEmpresa(Long empresaId);

    List<UserSummaryResponse> listUsersByEmpresa(Long empresaId);

    CreateEmpresaResponse createEmpresaWithSedes(CreateEmpresaRequest request);

    RegisterUserResponse createUserForEmpresa(CreateEmpresaUserRequest request);

    CreateEmpresaResponse updateEmpresaWithSedes(Long empresaId, UpdateEmpresaRequest request);

    UpsertTarifasResponse upsertTarifas(Long empresaId, Long sedeId, UpsertTarifasRequest request);

    void deleteEmpresa(Long empresaId);

    RegisterUserResponse updateUser(Long userId, UpdateUserRequest request);

    void deleteUser(Long userId);
}
