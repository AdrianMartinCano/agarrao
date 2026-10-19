package dev.pimon.listillo.gasto.dto;

import dev.pimon.listillo.generico.Categoria;

import java.math.BigDecimal;

/** Categoría null = las líneas sin genérico. */
public record CategoriaGastoDto(Categoria categoria, BigDecimal total) {
}
