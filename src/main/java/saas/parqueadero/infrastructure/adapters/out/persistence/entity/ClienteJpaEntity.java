package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cliente", indexes = {
    @Index(name = "idx_cliente_empresa", columnList = "empresa_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_cliente_empresa_telefono", columnNames = {"empresa_id", "telefono"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(name = "whatsapp_habilitado", nullable = false)
    private boolean whatsappHabilitado;

    @Column(name = "pref_facturacion", nullable = false)
    private boolean prefFacturacion;

    @Column(name = "pref_mensualidades", nullable = false)
    private boolean prefMensualidades;

    @Column(name = "pref_confirmaciones_pago", nullable = false)
    private boolean prefConfirmacionesPago;

    @Column(name = "pref_operativas", nullable = false)
    private boolean prefOperativas;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
