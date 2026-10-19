package dev.pimon.listillo.ticket.dto;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.OrigenTicket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo leído de un ticket para repasarlo antes de guardarlo. No se guarda nada hasta que la
 * persona lo confirma.
 *
 * @param texto el texto tal como se leyó (del PDF o del OCR), para ver qué ha entendido
 */
public record BorradorDto(
        Cadena cadena,
        String tiendaTexto,
        LocalDate fecha,
        BigDecimal total,
        OrigenTicket origen,
        List<LineaBorradorDto> lineas,
        List<String> avisos,
        String texto
) {
}
