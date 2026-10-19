package dev.pimon.listillo.ticket;

import java.util.UUID;

/** Se acaba de guardar un ticket en el hogar: hay productos nuevos que clasificar. */
public record TicketGuardado(UUID hogarId) {
}
