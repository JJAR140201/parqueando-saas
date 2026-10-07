package saas.parqueadero.domain.model;

import java.util.List;

/** Una pagina de resultados y el total de elementos que cumplen el filtro. */
public record Pagina<T>(List<T> contenido, long total) {
}
