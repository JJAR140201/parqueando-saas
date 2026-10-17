package saas.parqueadero.domain.port.out;

import java.util.Optional;
import saas.parqueadero.domain.model.WhatsappAccount;

public interface WhatsappAccountRepositoryPort {
    Optional<WhatsappAccount> findByEmpresaId(Long empresaId);

    Optional<WhatsappAccount> findByPhoneNumberId(String phoneNumberId);

    WhatsappAccount save(WhatsappAccount account);
}
