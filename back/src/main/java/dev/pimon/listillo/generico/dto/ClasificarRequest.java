package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.ticket.Cadena;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Corregir a mano el genérico de un producto (el texto del ticket en su cadena).
 *
 * @param genericoId null = no es ningún genérico
 * @param tamano     lo que trae una unidad comprada en la unidad del genérico; null = leerlo solo
 */
public record ClasificarRequest(
        @NotNull(message = "Falta la tienda") Cadena cadena,
        @NotBlank(message = "Falta el texto del ticket") @Size(max = 120) String texto,
        UUID genericoId,
        @Positive(message = "El tamaño tiene que ser mayor que cero")
        @DecimalMax(value = "10000", message = "Ese tamaño es demasiado grande") BigDecimal tamano
) {
}
