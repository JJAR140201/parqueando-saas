package saas.parqueadero.domain.port.out;

import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.Empresa;

public interface EmpresaRepositoryPort {
    List<Empresa> findAll();

    Optional<Empresa> findById(Long id);

    Optional<Empresa> findByNit(String nit);

    Empresa save(Empresa empresa);

    void deleteById(Long id);
}
