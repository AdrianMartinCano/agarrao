package dev.pimon.listillo.tienda.dto;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo encontrado en las webs de los súper, por cadena (la más barata primero).
 *
 * @param porUnidad true si todo se mide igual y se ha comparado por precio por unidad; si no,
 *                  por el precio del envase
 */
public record BusquedaDto(String texto, boolean porUnidad, List<Tienda> tiendas) {

    /** @param encontrados cuántos hay en esa cadena (se enseñan los más baratos) */
    public record Tienda(Cadena cadena, int encontrados, List<Producto> productos) {
    }

    /** @param fecha el día del precio; @param fuente WEB u OPEN_PRICES */
    public record Producto(String idExterno, String nombre, String formato, BigDecimal precio, BigDecimal tamano,
                           Unidad unidad, BigDecimal precioUnidad, String imagenUrl, String url,
                           java.time.LocalDate fecha, dev.pimon.listillo.generico.FuentePrecio fuente) {
    }
}
