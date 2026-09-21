package saas.parqueadero.licensing;

import java.time.LocalDate;

/**
 * Contenido de un serial de licencia ya decodificado y verificado.
 *
 * @param version   version del formato del serial
 * @param expiresAt fecha de expiracion de la licencia
 * @param nonce     identificador unico de la licencia (para el registro del super admin)
 */
public record LicenseSerialPayload(int version, LocalDate expiresAt, long nonce) {

    public boolean estaVencida(LocalDate hoy) {
        return hoy.isAfter(expiresAt);
    }

    public long diasRestantes(LocalDate hoy) {
        return java.time.temporal.ChronoUnit.DAYS.between(hoy, expiresAt);
    }
}
