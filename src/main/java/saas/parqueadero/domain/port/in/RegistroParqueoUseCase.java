package saas.parqueadero.domain.port.in;

import saas.parqueadero.application.dto.RegistrarEntradaRequest;
import saas.parqueadero.application.dto.RegistrarSalidaRequest;
import saas.parqueadero.application.dto.RegistroParqueoResponse;

public interface RegistroParqueoUseCase {
    RegistroParqueoResponse registrarEntrada(RegistrarEntradaRequest request);

    RegistroParqueoResponse registrarSalida(RegistrarSalidaRequest request);
}
