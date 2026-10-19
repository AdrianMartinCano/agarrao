package dev.pimon.listillo.gasolina.estaciones.dto;

import java.util.List;

/**
 * Página de resultados.
 *
 * Es un record propio y no un {@code Page} de Spring Data porque estos resultados no
 * salen de un repositorio, sino de filtrar una lista en memoria. Además, serializar
 * {@code PageImpl} directamente en la API está desaconsejado desde Spring Boot 3.3
 * (su JSON no es estable entre versiones).
 */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long total,
        int totalPaginas
) {

    public static <T> PaginaDto<T> de(List<T> todos, int pagina, int tamano) {
        int desde = Math.min(pagina * tamano, todos.size());
        int hasta = Math.min(desde + tamano, todos.size());
        int totalPaginas = (int) Math.ceil((double) todos.size() / tamano);

        return new PaginaDto<>(
                List.copyOf(todos.subList(desde, hasta)),
                pagina,
                tamano,
                todos.size(),
                totalPaginas
        );
    }
}
