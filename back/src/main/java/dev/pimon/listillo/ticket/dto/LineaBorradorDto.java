package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;

/** Una línea leída, todavía sin guardar, con su propuesta de casado (o null). */
public record LineaBorradorDto(
        String texto,
        BigDecimal cantidad,
        Unidad unidad,
        BigDecimal precioUnitario,
        BigDecimal importe,
        SugerenciaDto sugerencia
) {
}
