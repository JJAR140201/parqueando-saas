package saas.parqueadero.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Destinatario de las notificaciones de una empresa, con su consentimiento y preferencias. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    private Long id;
    private Long empresaId;
    private String nombre;
    /** Telefono en formato E.164 (por ejemplo +573001234567). */
    private String telefono;
    /** Consentimiento: solo se envia WhatsApp si es true. */
    private boolean whatsappHabilitado;
    private boolean prefFacturacion;
    private boolean prefMensualidades;
    private boolean prefConfirmacionesPago;
    private boolean prefOperativas;
    private Instant creadoEn;
    private Instant actualizadoEn;
}
