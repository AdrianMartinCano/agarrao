package dev.pimon.listillo.version.dto;

import dev.pimon.listillo.version.EstadoVersion;

/**
 * Respuesta a «tengo la versión X, ¿estoy al día?».
 *
 * La decisión ({@code estado}) la toma el back y no la app: así una regla nueva no obliga
 * a publicar otra versión de la app para que la entiendan las que ya están instaladas.
 */
public record ComprobacionVersionDto(
        EstadoVersion estado,
        String actual,
        String ultima,
        String minima,
        String notas
) {
}
