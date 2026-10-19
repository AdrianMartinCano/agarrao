package dev.pimon.listillo.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Para un código que Open Food Facts no conoce: lo que pone en el envase. */
public record NombrarProductoRequest(
        @NotBlank(message = "Escribe qué es")
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
        String nombre,

        @Size(max = 80, message = "La marca no puede pasar de 80 caracteres")
        String marca,

        @Size(max = 40, message = "La cantidad no puede pasar de 40 caracteres")
        String cantidad
) {
}
