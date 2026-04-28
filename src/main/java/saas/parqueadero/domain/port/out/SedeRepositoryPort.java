package saas.parqueadero.domain.port.out;

import java.util.Optional;
import java.util.List;
import saas.parqueadero.domain.model.Sede;

public interface SedeRepositoryPort {
    List<Sede> findByEmpresaId(Long empresaId);

    Optional<Sede> findByIdAndEmpresaId(Long id, Long empresaId);

    Sede save(Sede sede);

    void deleteByIdAndEmpresaId(Long id, Long empresaId);

    void deleteByEmpresaId(Long empresaId);
}
