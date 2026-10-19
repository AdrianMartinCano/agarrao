package dev.pimon.listillo.tienda.dto;

import dev.pimon.listillo.ticket.Cadena;

import java.util.List;

/**
 * Un producto concreto y lo que cuesta ese mismo producto en cada súper, del más barato al
 * más caro por precio por unidad.
 *
 * @param elegido el que se ha tocado
 * @param tiendas una fila por cadena (también la del elegido)
 */
public record EnCadaSuperDto(Cadena cadena, BusquedaDto.Producto elegido, List<Tienda> tiendas) {

    /**
     * @param mismoTamano false si en esa cadena no lo hay del mismo tamaño y se compara por
     *                    precio por unidad con otro envase
     */
    public record Tienda(Cadena cadena, BusquedaDto.Producto producto, boolean mismoTamano) {
    }
}
