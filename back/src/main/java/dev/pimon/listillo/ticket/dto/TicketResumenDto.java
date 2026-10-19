package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un ticket en el listado: lo justo para reconocerlo. */
public record TicketResumenDto(UUID id, Cadena cadena, LocalDate fecha, BigDecimal total, long lineas, long casadas) {
}
