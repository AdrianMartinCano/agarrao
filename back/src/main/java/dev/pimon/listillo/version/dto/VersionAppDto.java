package dev.pimon.listillo.version.dto;

import java.time.LocalDateTime;

public record VersionAppDto(
        String ultima,
        String minima,
        String notas,
        LocalDateTime actualizadaEn
) {
}
