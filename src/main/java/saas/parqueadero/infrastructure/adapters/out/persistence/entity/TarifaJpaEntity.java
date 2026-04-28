package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoVehiculo;

@Entity
@Table(name = "tarifa", indexes = {
    @Index(name = "idx_tarifa_empresa_sede", columnList = "empresa_id, sede_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tarifa_sede_empresa_tipo", columnNames = {"sede_id", "empresa_id", "tipo_vehiculo"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "valor_fraccion", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFraccion;

    @Column(name = "minutos_fraccion", nullable = false)
    private Integer minutosFraccion;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
}
