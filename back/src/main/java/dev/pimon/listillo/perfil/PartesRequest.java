package dev.pimon.listillo.perfil;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PartesRequest(
        @NotEmpty(message = "Deja encendida al menos una parte")
        List<@NotNull Parte> partes
) {
}
