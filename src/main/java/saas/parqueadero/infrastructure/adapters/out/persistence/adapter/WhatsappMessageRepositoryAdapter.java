package saas.parqueadero.infrastructure.adapters.out.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import saas.parqueadero.domain.model.Pagina;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.domain.port.out.WhatsappMessageRepositoryPort;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.WhatsappMessageJpaEntity;
import saas.parqueadero.infrastructure.adapters.out.persistence.mapper.WhatsappMessagePersistenceMapper;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.WhatsappMessageJpaRepository;

@Component
@RequiredArgsConstructor
public class WhatsappMessageRepositoryAdapter implements WhatsappMessageRepositoryPort {

    private final WhatsappMessageJpaRepository repository;
    private final WhatsappMessagePersistenceMapper mapper;

    @Override
    public WhatsappMessage save(WhatsappMessage message) {
        return mapper.toDomain(repository.save(mapper.toEntity(message)));
    }

    @Override
    public Optional<WhatsappMessage> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<WhatsappMessage> findByProviderMessageId(String providerMessageId) {
        return repository.findByProviderMessageId(providerMessageId).map(mapper::toDomain);
    }

    @Override
    public Pagina<WhatsappMessage> findPagina(Long empresaId, WhatsappMessageStatus estado, TipoNotificacionWhatsapp tipo,
        Instant desde, Instant hasta, int pagina, int tamano) {
        Page<WhatsappMessageJpaEntity> resultado =
            repository.buscar(empresaId, estado, tipo, desde, hasta, PageRequest.of(pagina, tamano));
        return new Pagina<>(resultado.getContent().stream().map(mapper::toDomain).toList(), resultado.getTotalElements());
    }

    @Override
    public List<WhatsappMessage> findParaMetricas(Long empresaId, Instant desde, Instant hasta, int limite) {
        return repository.paraMetricas(empresaId, desde, hasta, PageRequest.of(0, limite)).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
