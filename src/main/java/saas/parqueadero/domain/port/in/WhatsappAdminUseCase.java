package saas.parqueadero.domain.port.in;

import java.time.LocalDate;
import saas.parqueadero.application.dto.UpsertWhatsappCuentaRequest;
import saas.parqueadero.application.dto.WhatsappCuentaResponse;
import saas.parqueadero.application.dto.WhatsappMensajeResponse;
import saas.parqueadero.application.dto.WhatsappMensajesPaginaResponse;
import saas.parqueadero.application.dto.WhatsappMetricasResponse;
import saas.parqueadero.application.dto.WhatsappPruebaRequest;
import saas.parqueadero.domain.model.TipoNotificacionWhatsapp;
import saas.parqueadero.domain.model.WhatsappMessageStatus;

public interface WhatsappAdminUseCase {

    int TAMANO_MAXIMO_PAGINA = 200;

    /** @return la cuenta de la empresa, o null si todavia no se configuro. */
    WhatsappCuentaResponse obtenerCuenta(Long empresaId);

    WhatsappCuentaResponse guardarCuenta(Long empresaId, UpsertWhatsappCuentaRequest request);

    /** Envia un mensaje de plantilla de ejemplo al telefono indicado. */
    WhatsappMensajeResponse enviarPrueba(Long empresaId, WhatsappPruebaRequest request);

    WhatsappMensajesPaginaResponse listarMensajes(Long empresaId, WhatsappMessageStatus estado, TipoNotificacionWhatsapp tipo,
        LocalDate desde, LocalDate hasta, int pagina, int tamano);

    WhatsappMetricasResponse metricas(Long empresaId, LocalDate desde, LocalDate hasta);
}
