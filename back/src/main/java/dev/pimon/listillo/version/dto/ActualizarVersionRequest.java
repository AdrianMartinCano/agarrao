package dev.pimon.listillo.version.dto;

import dev.pimon.listillo.version.Semver;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarVersionRequest(
        @NotBlank @Pattern(regexp = Semver.PATRON, message = "debe ser x.y.z") String ultima,
        @NotBlank @Pattern(regexp = Semver.PATRON, message = "debe ser x.y.z") String minima,
        @Size(max = 500) String notas
) {
}
