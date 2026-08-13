package saas.parqueadero.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token", indexes = {
    @Index(name = "idx_refresh_token_usuario", columnList = "usuario_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = {"token_hash"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
