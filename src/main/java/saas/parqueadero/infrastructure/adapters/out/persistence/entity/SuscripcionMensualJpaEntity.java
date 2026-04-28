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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.TipoVehiculo;

@Entity
@Table(name = "suscripcion_mensual", indexes = {
    @Index(name = "idx_suscripcion_placa_empresa_sede", columnList = "placa, empresa_id, sede_id"),
    @Index(name = "idx_suscripcion_activa", columnList = "activa")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuscripcionMensualJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "valor_mensual", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorMensual;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Boolean activa;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
}
