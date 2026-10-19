package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Textos;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Una línea de ticket con lo que hace falta de su ticket: de aquí salen precios y gasto. */
public record Compra(UUID lineaId, UUID ticketId, LocalDate fecha, Cadena cadena, String texto,
                     BigDecimal cantidad, Unidad unidad, BigDecimal importe, String ean, String nombre) {

    /** El producto, como lo identifica la clasificación: el texto normalizado en su cadena. */
    public Clasificacion.Clave clave(UUID hogarId) {
        return new Clasificacion.Clave(hogarId, cadena, Textos.normalizar(texto));
    }
}
