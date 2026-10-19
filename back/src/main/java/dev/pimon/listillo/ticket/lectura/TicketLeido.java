package dev.pimon.listillo.ticket.lectura;

import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lo que se ha entendido de un ticket. Fecha y total pueden faltar si no se leyeron. */
public record TicketLeido(
        Cadena cadena,
        String tiendaTexto,
        LocalDate fecha,
        BigDecimal total,
        List<LineaLeida> lineas,
        List<String> avisos
) {
}
