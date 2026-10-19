package dev.pimon.listillo.ticket.dto;

/** El ticket guardado y cuántas cosas de la lista se han tachado con él. */
public record GuardadoDto(TicketDto ticket, int tachados) {
}
