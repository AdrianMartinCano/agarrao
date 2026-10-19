package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.ticket.Cadena;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Corregir el genérico de un producto del catálogo web de una cadena. Vale para todos los
 * hogares (el catálogo web es común), así que solo admite genéricos del catálogo de Listillo.
 *
 * @param genericoId null = no es ningún genérico
 * @param quitarDe   el genérico propio desde el que se dice «No es esto» (solo se quita de ese)
 */
public record ClasificarWebRequest(
        @NotNull(message = "Falta la tienda") Cadena cadena,
        @NotBlank(message = "Falta el producto") @Size(max = 40) String idExterno,
        UUID genericoId,
        UUID quitarDe
) {
}
