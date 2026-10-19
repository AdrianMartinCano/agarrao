package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.gasto.TipoGasto;

import java.math.BigDecimal;

/** Lo gastado en un tipo en el mes; {@code veces}: tickets más gastos anotados. */
public record TipoGastoDto(TipoGasto tipo, BigDecimal total, int veces) {
}
