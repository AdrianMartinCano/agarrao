package dev.pimon.listillo.tienda;

import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;

/**
 * Un producto tal como lo lee el importador de una web: precio, lo que trae (en kilos,
 * litros o piezas) y su precio por esa unidad.
 */
public record ProductoWeb(String idExterno, String nombre, String formato, BigDecimal precio, BigDecimal tamano,
                          Unidad unidad, BigDecimal precioUnidad, String seccion, String imagenUrl, String url) {
}
