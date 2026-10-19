package dev.pimon.listillo.hogar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnirseRequest(
        @NotBlank(message = "Escribe el código de invitación")
        @Size(max = 20, message = "Ese código es demasiado largo")
        String codigo
) {
}
