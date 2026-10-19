package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.ticket.Cadena;

import java.math.BigDecimal;

public record TiendaGastoDto(Cadena cadena, BigDecimal total, int tickets) {
}
