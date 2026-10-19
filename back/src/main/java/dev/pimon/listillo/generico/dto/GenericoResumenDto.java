package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.generico.FuentePrecio;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un genérico en el catálogo con su mejor precio por unidad (de las webs de los súper o de
 * los tickets recientes del hogar). Sin ninguno, precio, cadena y fuente van a null (la
 * ficha sale en gris).
 *
 * @param productos    productos de este genérico comprados por el hogar
 * @param productosWeb productos de este genérico en las webs de los súper
 */
public record GenericoResumenDto(UUID id, String nombre, Categoria categoria, Unidad unidad, String retrato,
                                 boolean propio, BigDecimal mejorPrecio, Cadena mejorCadena, FuentePrecio mejorFuente,
                                 int productos, int productosWeb, LocalDate ultimaCompra) {
}
