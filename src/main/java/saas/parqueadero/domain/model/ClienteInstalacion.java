package saas.parqueadero.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una instalacion del sistema de escritorio en el equipo de un cliente.
 *
 * <p>El proveedor la da de alta desde la consola y obtiene {@code tenantId} +
 * un token de sincronizacion (que se entrega una sola vez, aqui solo vive su
 * hash). La instalacion usa esas credenciales para subir snapshots de sus datos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInstalacion {

    private Long id;

    /** Identificador publico y estable de la instalacion (UUID). */
    private String tenantId;

    /** Nombre legible del cliente/parqueadero. */
    private String nombre;

    /** Empresa asociada en el modelo multi-tenant (opcional). */
    private Long empresaId;

    /** SHA-256 (hex) del token de sincronizacion. El token en claro no se guarda. */
    private String tokenHash;

    /** Si esta en {@code false}, se rechazan los snapshots entrantes. */
    private boolean activa;

    private Instant creadaEn;

    /** Ultima vez que llego un snapshot valido de esta instalacion. */
    private Instant ultimaSyncEn;

    /** Version de la app de escritorio reportada en el ultimo snapshot. */
    private String appVersion;

    /** Estado de la licencia local reportado en el ultimo snapshot. */
    private String estadoLicencia;

    /** Fecha de expiracion de la licencia local reportada en el ultimo snapshot. */
    private LocalDate licenciaExpiraEn;

    /** Cifras del ultimo snapshot, para el tablero. */
    private ResumenOperativo ultimoResumen;
}
