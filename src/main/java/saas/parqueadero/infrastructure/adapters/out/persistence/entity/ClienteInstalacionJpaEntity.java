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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cliente_instalacion", indexes = {
    @Index(name = "idx_cliente_instalacion_tenant", columnList = "tenant_id", unique = true),
    @Index(name = "idx_cliente_instalacion_empresa", columnList = "empresa_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInstalacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "ultima_sync_en")
    private Instant ultimaSyncEn;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "estado_licencia", length = 40)
    private String estadoLicencia;

    @Column(name = "licencia_expira_en")
    private LocalDate licenciaExpiraEn;

    @Column(name = "ultimo_vehiculos_dentro")
    private Integer ultimoVehiculosDentro;

    @Column(name = "ultimo_entradas_hoy")
    private Integer ultimoEntradasHoy;

    @Column(name = "ultimo_salidas_hoy")
    private Integer ultimoSalidasHoy;

    @Column(name = "ultimo_recaudo_hoy", precision = 16, scale = 2)
    private BigDecimal ultimoRecaudoHoy;
}
