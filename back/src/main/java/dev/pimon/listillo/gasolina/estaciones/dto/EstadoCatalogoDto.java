package dev.pimon.listillo.gasolina.estaciones.dto;

import java.time.Instant;

/**
 * Salud de los datos que sirve la API.
 *
 * @param disponible          false mientras no haya habido una primera descarga correcta
 * @param estaciones          cuántas hay cargadas ahora mismo
 * @param ultimaActualizacion cuándo se descargaron; null si aún no hay datos. El front
 *                            lo usa para avisar de que los precios pueden estar viejos.
 */
public record EstadoCatalogoDto(
        boolean disponible,
        int estaciones,
        Instant ultimaActualizacion
) {
}
