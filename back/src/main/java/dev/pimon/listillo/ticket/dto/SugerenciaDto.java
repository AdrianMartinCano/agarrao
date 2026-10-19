package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.OrigenSugerencia;

/** Con qué propone Listillo casar una línea. {@code ean} es null si es solo un nombre de la lista. */
public record SugerenciaDto(String ean, String nombre, OrigenSugerencia origen) {
}
