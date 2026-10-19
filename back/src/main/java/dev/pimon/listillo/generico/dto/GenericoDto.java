package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Unidad;

import java.util.UUID;

/** Un genérico recién creado. */
public record GenericoDto(UUID id, String nombre, Categoria categoria, Unidad unidad, String retrato, boolean propio) {
}
