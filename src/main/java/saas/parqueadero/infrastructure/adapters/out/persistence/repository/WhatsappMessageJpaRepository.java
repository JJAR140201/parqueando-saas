package saas.parqueadero.infrastructure.adapters.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessageStatus;
import saas.parqueadero.infrastructure.adapters.out.persistence.entity.WhatsappMessageJpaEntity;

public interface WhatsappMessageJpaRepository extends JpaRepository<WhatsappMessageJpaEntity, Long> {
    Optional<WhatsappMessageJpaEntity> findByProviderMessageId(String providerMessageId);

    @Query("""
        select m from WhatsappMessageJpaEntity m
        where m.empresaId = :empresaId
          and (:estado is null or m.status = :estado)
          and (:tipo is null or m.messageType = :tipo)
          and m.requestedAt >= coalesce(:desde, m.requestedAt)
          and m.requestedAt <= coalesce(:hasta, m.requestedAt)
        order by m.requestedAt desc, m.id desc
        """)
    Page<WhatsappMessageJpaEntity> buscar(
        @Param("empresaId") Long empresaId,
        @Param("estado") WhatsappMessageStatus estado,
        @Param("tipo") TipoNotificacionWhatsapp tipo,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable);

    @Query("""
        select m from WhatsappMessageJpaEntity m
        where m.empresaId = :empresaId
          and m.requestedAt >= coalesce(:desde, m.requestedAt)
          and m.requestedAt <= coalesce(:hasta, m.requestedAt)
        order by m.requestedAt desc
        """)
    List<WhatsappMessageJpaEntity> paraMetricas(
        @Param("empresaId") Long empresaId,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable);
}
