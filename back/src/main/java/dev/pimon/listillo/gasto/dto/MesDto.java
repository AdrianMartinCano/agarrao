package dev.pimon.listillo.gasto.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MesDto(YearMonth mes, BigDecimal total, int tickets) {
}
