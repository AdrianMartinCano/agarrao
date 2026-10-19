package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un producto de fuera (la web de una cadena u Open Prices) que es este genérico.
 *
 * @param actualizado el día del precio
 */
public record ProductoWebDto(Cadena cadena, String idExterno, String nombre, String formato, BigDecimal precio,
                             BigDecimal tamano, BigDecimal precioUnidad, String imagenUrl, String url,
                             LocalDate actualizado, dev.pimon.listillo.generico.FuentePrecio fuente) {
}
