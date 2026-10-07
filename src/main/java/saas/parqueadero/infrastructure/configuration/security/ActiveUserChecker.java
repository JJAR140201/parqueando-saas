package saas.parqueadero.infrastructure.configuration.security;

import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import saas.parqueadero.infrastructure.adapters.out.persistence.repository.UsuarioJpaRepository;

/**
 * Comprueba que el usuario de un JWT siga existiendo. El resultado se cachea unos segundos para
 * no consultar la BD en cada peticion; un usuario eliminado deja de poder operar en menos de un minuto
 * en vez de esperar a que expire su access token.
 */
@Component
@RequiredArgsConstructor
public class ActiveUserChecker {

    private static final long TTL_MILLIS = 60_000L;
    private static final int MAX_ENTRADAS = 10_000;

    private record Entrada(boolean existe, long hastaMillis) {
    }

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final ConcurrentHashMap<Long, Entrada> cache = new ConcurrentHashMap<>();

    public boolean isActive(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        long ahora = System.currentTimeMillis();
        Entrada entrada = cache.get(usuarioId);
        if (entrada != null && entrada.hastaMillis() > ahora) {
            return entrada.existe();
        }
        boolean existe = usuarioJpaRepository.existsById(usuarioId);
        if (cache.size() > MAX_ENTRADAS) {
            cache.entrySet().removeIf(e -> e.getValue().hastaMillis() <= ahora);
        }
        cache.put(usuarioId, new Entrada(existe, ahora + TTL_MILLIS));
        return existe;
    }

    /** Descarta el estado cacheado de un usuario (por ejemplo, tras eliminarlo). */
    public void invalidate(Long usuarioId) {
        cache.remove(usuarioId);
    }
}
