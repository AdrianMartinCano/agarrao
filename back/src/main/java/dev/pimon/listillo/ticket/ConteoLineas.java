package dev.pimon.listillo.ticket;

import java.util.UUID;

public record ConteoLineas(UUID ticketId, long lineas, long casadas) {
}
