package saas.parqueadero.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import saas.parqueadero.domain.model.Pagina;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessage;
import saas.parqueadero.domain.model.WhatsappMessageStatus;

public interface WhatsappMessageRepositoryPort {
    WhatsappMessage save(WhatsappMessage message);

    Optional<WhatsappMessage> findById(Long id);

    Optional<WhatsappMessage> findByProviderMessageId(String providerMessageId);

    /** Mensajes de una empresa, del mas reciente al mas antiguo. Los filtros nulos no aplican. */
    Pagina<WhatsappMessage> findPagina(Long empresaId, WhatsappMessageStatus estado, TipoNotificacionWhatsapp tipo,
        Instant desde, Instant hasta, int pagina, int tamano);

    /** Mensajes de una empresa solicitados en el rango, hasta {@code limite}, para calcular metricas. */
    List<WhatsappMessage> findParaMetricas(Long empresaId, Instant desde, Instant hasta, int limite);
}
