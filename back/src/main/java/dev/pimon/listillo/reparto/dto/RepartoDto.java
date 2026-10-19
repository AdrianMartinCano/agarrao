package dev.pimon.listillo.reparto.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.generico.FuentePrecio;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * La lista pendiente repartida por súper: cada cosa en la cadena donde su genérico sale
 * más barato por unidad.
 *
 * @param grupos    una cadena por grupo, de la que más se lleva a la que menos
 * @param sinPrecio lo que no tiene genérico o no tiene precio en ninguna cadena
 * @param total     lo que costaría comprando cada cosa donde sale más barata
 * @param enUna     lo que costaría ir solo a una tienda (con lo que allí no hay)
 */
public record RepartoDto(List<Grupo> grupos, List<SinPrecio> sinPrecio, BigDecimal total, List<EnUna> enUna) {

    public record Grupo(Cadena cadena, BigDecimal total, List<Articulo> articulos) {
    }

    /**
     * @param producto el producto más barato de esa cadena («Leche semidesnatada Hacendado»)
     * @param importe  lo que cuesta la cantidad de su envase más barato (por lo apuntado)
     */
    public record Articulo(UUID articuloId, String nombre, int cantidad, UUID genericoId, String generico,
                           Categoria categoria, String retrato, String producto, BigDecimal precioUnidad,
                           Unidad unidad, BigDecimal importe, FuentePrecio fuente) {
    }

    /** @param generico null si no se sabe qué es */
    public record SinPrecio(UUID articuloId, String nombre, String generico) {
    }

    /** @param faltan cuántas cosas con precio en otras tiendas no tienen precio en esta */
    public record EnUna(Cadena cadena, BigDecimal total, int faltan) {
    }
}
