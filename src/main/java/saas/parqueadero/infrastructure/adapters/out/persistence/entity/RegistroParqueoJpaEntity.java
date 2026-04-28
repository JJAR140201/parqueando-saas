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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import saas.parqueadero.domain.model.EstadoRegistroParqueo;
import saas.parqueadero.domain.model.TipoVehiculo;

@Entity
@Table(name = "registro_parqueo", indexes = {
    @Index(name = "idx_registro_placa_sede_empresa", columnList = "placa, sede_id, empresa_id"),
    @Index(name = "idx_registro_estado", columnList = "estado")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroParqueoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private TipoVehiculo tipoVehiculo;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDateTime fechaEntrada;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegistroParqueo estado;

    @Column(name = "total_pagado", precision = 14, scale = 2)
    private BigDecimal totalPagado;

    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
}
