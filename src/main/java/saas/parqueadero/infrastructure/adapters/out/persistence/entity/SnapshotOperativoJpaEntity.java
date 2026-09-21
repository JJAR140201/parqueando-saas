package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "snapshot_operativo", indexes = {
    @Index(name = "idx_snapshot_tenant_recibido", columnList = "tenant_id, recibido_en")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotOperativoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    @Column(name = "generado_en")
    private Instant generadoEn;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "estado_licencia", length = 40)
    private String estadoLicencia;

    @Column(name = "vehiculos_dentro")
    private Integer vehiculosDentro;

    @Column(name = "entradas_hoy")
    private Integer entradasHoy;

    @Column(name = "salidas_hoy")
    private Integer salidasHoy;

    @Column(name = "recaudo_hoy", precision = 16, scale = 2)
    private BigDecimal recaudoHoy;

    /** Volcado completo de las tablas del escritorio (Postgres jsonb). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payloadJson;
}
