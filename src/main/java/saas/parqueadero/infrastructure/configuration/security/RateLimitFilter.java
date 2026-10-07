package saas.parqueadero.infrastructure.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita por IP las peticiones a los endpoints publicos (login, licencias, sync) y rechaza
 * cuerpos de sincronizacion demasiado grandes antes de leerlos. Estado en memoria.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Regla(String grupo, int maxPeticiones, Duration ventana) {
    }

    private static final Map<String, Regla> REGLAS = Map.of(
        "/api/v1/auth/login", new Regla("login", 10, Duration.ofMinutes(5)),
        "/api/v1/auth/refresh", new Regla("refresh", 60, Duration.ofMinutes(5)),
        "/api/v1/licencias/validar", new Regla("licencias", 20, Duration.ofMinutes(10)),
        "/api/v1/licencias/redimir", new Regla("licencias", 20, Duration.ofMinutes(10)),
        "/api/v1/licencias/renovar", new Regla("licencias", 20, Duration.ofMinutes(10)),
        "/api/v1/sync/snapshot", new Regla("sync", 30, Duration.ofMinutes(1))
    );

    private static final long LIMPIEZA_MILLIS = Duration.ofMinutes(10).toMillis();

    private record Contador(long inicioMillis, int cuenta) {
    }

    private final ConcurrentHashMap<String, Contador> contadores = new ConcurrentHashMap<>();
    private final long maxPayloadBytes;

    public RateLimitFilter(@Value("${app.sync.max-payload-bytes:10485760}") long maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        Regla regla = "POST".equals(request.getMethod()) ? REGLAS.get(request.getRequestURI()) : null;
        if (regla == null) {
            chain.doFilter(request, response);
            return;
        }

        if ("sync".equals(regla.grupo()) && request.getContentLengthLong() > maxPayloadBytes) {
            rechazar(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "El cuerpo de la peticion excede el tamano permitido");
            return;
        }

        if (!permitir(regla.grupo() + "|" + resolverIp(request), regla)) {
            response.setHeader("Retry-After", String.valueOf(regla.ventana().toSeconds()));
            rechazar(response, 429, "TOO_MANY_REQUESTS", "Demasiadas peticiones. Intenta de nuevo en unos minutos");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean permitir(String clave, Regla regla) {
        long ahora = System.currentTimeMillis();
        long ventanaMillis = regla.ventana().toMillis();
        Contador resultado = contadores.merge(clave, new Contador(ahora, 1), (previo, nuevo) ->
            ahora - previo.inicioMillis() >= ventanaMillis
                ? nuevo
                : new Contador(previo.inicioMillis(), previo.cuenta() + 1));
        if (contadores.size() > 10_000) {
            contadores.entrySet().removeIf(e -> ahora - e.getValue().inicioMillis() >= LIMPIEZA_MILLIS);
        }
        return resultado.cuenta() <= regla.maxPeticiones();
    }

    /**
     * Tras el proxy de Railway, la IP real es la ultima entrada de X-Forwarded-For (la que
     * agrego el proxy); las anteriores las puede falsear el cliente.
     */
    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] partes = forwarded.split(",");
            return partes[partes.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    private void rechazar(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
