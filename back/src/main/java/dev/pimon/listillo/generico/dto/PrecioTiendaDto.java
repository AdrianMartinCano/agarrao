package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.FuentePrecio;
import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo más barato de un genérico en una cadena. Si la cadena tiene catálogo web, manda su
 * precio de hoy; si no, el último de los tickets del hogar.
 *
 * @param nombre  el producto («Leche semidesnatada Hacendado» o el texto del ticket)
 * @param tamano  lo que trae, en la unidad del genérico (null si no se sabe)
 * @param importe lo que cuesta el envase (o lo que se pagó en el ticket)
 * @param url     la ficha del producto en la web de la cadena (solo si sale de la web)
 */
public record PrecioTiendaDto(Cadena cadena, BigDecimal precio, LocalDate fecha, FuentePrecio fuente, String nombre,
                              BigDecimal tamano, BigDecimal importe, String url, String imagenUrl) {
}
