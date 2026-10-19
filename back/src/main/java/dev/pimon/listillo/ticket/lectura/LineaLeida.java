package dev.pimon.listillo.ticket.lectura;

import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;

/**
 * Una línea de artículo tal como sale del ticket.
 *
 * @param texto          lo que pone («LECHE SEMI HACEND 6X1L»)
 * @param cantidad       piezas, kilos o litros
 * @param precioUnitario por pieza, kilo o litro; puede no venir si el ticket no lo da
 * @param importe        lo que se pagó por la línea (negativo en un descuento)
 */
public record LineaLeida(String texto, BigDecimal cantidad, Unidad unidad, BigDecimal precioUnitario, BigDecimal importe) {
}
