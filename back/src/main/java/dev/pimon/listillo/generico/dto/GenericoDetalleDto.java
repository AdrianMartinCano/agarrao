package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Unidad;

import java.util.List;
import java.util.UUID;

/**
 * Un genérico con su comparación: la mejor tienda primero, lo comprado por el hogar y lo
 * que hay en las webs de los súper (los más baratos primero).
 */
public record GenericoDetalleDto(UUID id, String nombre, Categoria categoria, Unidad unidad, String retrato,
                                 boolean propio, List<PrecioTiendaDto> tiendas, List<ProductoDeGenericoDto> productos,
                                 List<ProductoWebDto> enLaWeb) {
}
