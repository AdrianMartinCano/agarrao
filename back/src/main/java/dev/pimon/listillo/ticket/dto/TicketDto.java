package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.OrigenTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TicketDto(
        UUID id,
        Cadena cadena,
        String tiendaTexto,
        LocalDate fecha,
        BigDecimal total,
        OrigenTicket origen,
        List<LineaTicketDto> lineas
) {
}
