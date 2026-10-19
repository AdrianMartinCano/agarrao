package dev.pimon.listillo.hogar.dto;

import dev.pimon.listillo.ticket.Cadena;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Los supermercados que la casa quiere ver y comparar. */
public record CadenasRequest(@NotEmpty(message = "Elige al menos un supermercado") List<Cadena> cadenas) {
}
