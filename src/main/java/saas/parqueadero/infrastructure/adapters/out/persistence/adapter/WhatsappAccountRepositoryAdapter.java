package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.WhatsappAccount;
import saas.parqueadero.domain.port.out.WhatsappAccountRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.WhatsappAccountPersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.WhatsappAccountJpaRepository;

@Component
@RequiredArgsConstructor
public class WhatsappAccountRepositoryAdapter implements WhatsappAccountRepositoryPort {

    private final WhatsappAccountJpaRepository repository;
    private final WhatsappAccountPersistenceMapper mapper;

    @Override
    public Optional<WhatsappAccount> findByEmpresaId(Long empresaId) {
        return repository.findByEmpresaId(empresaId).map(mapper::toDomain);
    }

    @Override
    public Optional<WhatsappAccount> findByPhoneNumberId(String phoneNumberId) {
        return repository.findByPhoneNumberId(phoneNumberId).map(mapper::toDomain);
    }

    @Override
    public WhatsappAccount save(WhatsappAccount account) {
        return mapper.toDomain(repository.save(mapper.toEntity(account)));
    }
}
