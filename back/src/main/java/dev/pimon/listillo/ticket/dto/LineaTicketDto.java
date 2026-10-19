package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaTicketDto(
        UUID id,
        String texto,
        BigDecimal cantidad,
        Unidad unidad,
        BigDecimal precioUnitario,
        BigDecimal importe,
        String ean,
        String nombre
) {
}
