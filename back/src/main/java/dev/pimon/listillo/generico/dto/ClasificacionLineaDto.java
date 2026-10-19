package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.generico.OrigenClasificacion;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El genérico de una línea de ticket. Sin clasificar: todo a null salvo {@code lineaId}.
 * Clasificada a mano como «ninguno»: {@code genericoId} null y origen USUARIO.
 */
public record ClasificacionLineaDto(UUID lineaId, UUID genericoId, String generico, Categoria categoria,
                                    Unidad unidad, String retrato, BigDecimal tamano, BigDecimal precioUnidad,
                                    OrigenClasificacion origen) {
}
