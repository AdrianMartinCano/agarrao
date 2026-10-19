package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.OrigenTicket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El ticket ya repasado. Sin total, se usa la suma de las líneas. */
public record GuardarTicketRequest(
        @NotNull(message = "Falta la tienda") Cadena cadena,
        @Size(max = 80) String tiendaTexto,
        @NotNull(message = "Falta la fecha") @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
        BigDecimal total,
        @NotNull OrigenTicket origen,
        @NotEmpty(message = "El ticket necesita al menos una línea")
        @Size(max = 200, message = "Demasiadas líneas para un ticket")
        List<@Valid LineaRequest> lineas
) {
}
