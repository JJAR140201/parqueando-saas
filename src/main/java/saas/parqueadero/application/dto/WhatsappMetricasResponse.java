package saas.parqueadero.application.dto;

import java.util.Map;

/** Resumen de mensajes y latencias de un rango. Las latencias estan en milisegundos. */
public record WhatsappMetricasResponse(
    long totalMensajes,
    Map<String, Long> porEstado,
    Latencia latenciaApi,
    Latencia latenciaEnvio,
    Latencia latenciaEntrega
) {
    public record Latencia(int muestras, Long promedioMs, Long p50Ms, Long p95Ms) {
    }
}
