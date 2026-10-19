package dev.pimon.listillo.hogar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Para crear un hogar y para cambiarle el nombre. */
public record NombreHogarRequest(
        @NotBlank(message = "Ponle un nombre al hogar")
        @Size(max = 40, message = "El nombre no puede pasar de 40 caracteres")
        String nombre
) {
}
