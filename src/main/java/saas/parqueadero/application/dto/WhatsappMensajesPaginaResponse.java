package saas.parqueadero.application.dto;

import java.util.List;

public record WhatsappMensajesPaginaResponse(List<WhatsappMensajeResponse> items, long total) {
}
