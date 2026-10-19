package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.OrigenClasificacion;
import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un producto comprado de este genérico: el texto del ticket en su cadena, con su última
 * compra. {@code tamano} y {@code precioUnidad} van a null si no se sabe cuánto trae.
 *
 * @param texto  como lo pone el ticket, para corregirlo (identifica el producto en su cadena)
 * @param nombre el mejor nombre que se sabe: el de Open Food Facts, el de la lista o el texto
 */
public record ProductoDeGenericoDto(Cadena cadena, String texto, String nombre, String marca, BigDecimal tamano,
                                    BigDecimal precioUnidad, BigDecimal importe, LocalDate fecha, int compras,
                                    OrigenClasificacion origen) {
}
