package dev.pimon.listillo.lista.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sin cantidad = 1. */
public record NuevoArticuloRequest(
        @NotBlank(message = "Escribe qué hay que comprar")
        @Size(max = 80, message = "No puede pasar de 80 caracteres")
        String nombre,

        @Min(value = 1, message = "La cantidad va de 1 a 99")
        @Max(value = 99, message = "La cantidad va de 1 a 99")
        Integer cantidad,

        /** Si se apuntó escaneando. */
        @Pattern(regexp = "[0-9]{8,14}", message = "Ese código de barras no es válido")
        String ean
) {
}
