package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.generico.Categoria;

import java.math.BigDecimal;
import java.util.UUID;

public record GenericoGastoDto(UUID id, String nombre, Categoria categoria, String retrato, BigDecimal total,
                               int compras) {
}
