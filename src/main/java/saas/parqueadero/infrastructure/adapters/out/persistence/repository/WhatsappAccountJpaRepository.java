package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.WhatsappAccountJpaEntity;

public interface WhatsappAccountJpaRepository extends JpaRepository<WhatsappAccountJpaEntity, Long> {
    Optional<WhatsappAccountJpaEntity> findByEmpresaId(Long empresaId);

    Optional<WhatsappAccountJpaEntity> findByPhoneNumberId(String phoneNumberId);
}
