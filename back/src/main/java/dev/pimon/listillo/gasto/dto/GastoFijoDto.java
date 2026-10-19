package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.gasto.TipoGasto;

import java.math.BigDecimal;
import java.util.UUID;

/** Lo que se apunta solo cada mes, el día {@code dia}. */
public record GastoFijoDto(UUID id, String concepto, BigDecimal importe, TipoGasto tipo, int dia) {
}
