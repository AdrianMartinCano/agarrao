package dev.pimon.listillo.producto.dto;

import dev.pimon.listillo.producto.OrigenProducto;

/**
 * @param fuente lo que hay que citar al enseñarlo («Open Food Facts», licencia ODbL), o
 *               null si lo escribió alguien de Listillo
 */
public record ProductoDto(
        String ean,
        String nombre,
        String marca,
        String cantidad,
        String imagenUrl,
        OrigenProducto origen,
        String fuente
) {
}
