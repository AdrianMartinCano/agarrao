package dev.pimon.listillo.generico.dto;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Unidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un genérico propio del hogar. */
public record NuevoGenericoRequest(
        @NotBlank(message = "Ponle un nombre") @Size(max = 60, message = "Nombre demasiado largo") String nombre,
        @NotNull(message = "Elige una categoría") Categoria categoria,
        @NotNull(message = "Elige cómo se compara") Unidad unidad
) {
}
