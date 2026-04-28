package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sede", indexes = {
    @Index(name = "idx_sede_empresa", columnList = "empresa_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SedeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "capacidad_actual", nullable = false)
    private Integer capacidadActual;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
}
