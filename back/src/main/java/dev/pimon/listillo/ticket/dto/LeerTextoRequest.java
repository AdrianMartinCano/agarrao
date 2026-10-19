package dev.pimon.listillo.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El texto de una foto de ticket, leído en el móvil (Tesseract). */
public record LeerTextoRequest(
        @NotBlank(message = "No ha llegado texto del ticket")
        @Size(max = 20000, message = "Demasiado texto para un ticket")
        String texto
) {
}
