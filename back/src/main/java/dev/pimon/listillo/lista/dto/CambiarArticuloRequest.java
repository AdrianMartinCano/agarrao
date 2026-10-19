package dev.pimon.listillo.lista.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cambio parcial: lo que venga a null se deja como está. */
public record CambiarArticuloRequest(
        @Size(max = 80, message = "No puede pasar de 80 caracteres")
        @Pattern(regexp = "(?s).*\\S.*", message = "El nombre no puede quedar vacío")
        String nombre,

        @Min(value = 1, message = "La cantidad va de 1 a 99")
        @Max(value = 99, message = "La cantidad va de 1 a 99")
        Integer cantidad,

        Boolean comprado
) {
}
