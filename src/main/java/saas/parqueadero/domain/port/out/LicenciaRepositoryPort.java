package saas.parqueadero.domain.port.out;

import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.Licencia;

public interface LicenciaRepositoryPort {
    Optional<Licencia> findById(Long id);

    Optional<Licencia> findByCodigo(String codigoNormalizado);

    Optional<Licencia> findByEmpresaId(Long empresaId);

    List<Licencia> findAll();

    Licencia save(Licencia licencia);
}
