package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.gasto.TipoGasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un gasto anotado. {@code fijoId}: el gasto fijo del que salió (Netflix de octubre), si salió de uno. */
public record GastoAnotadoDto(UUID id, String concepto, BigDecimal importe, LocalDate fecha, TipoGasto tipo,
                              UUID fijoId) {
}
