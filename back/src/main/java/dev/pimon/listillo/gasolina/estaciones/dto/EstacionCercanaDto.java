package dev.pimon.listillo.gasolina.estaciones.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Una estación como la ve el front, ya con la distancia calculada.
 *
 * @param distanciaKm  en línea recta desde el punto buscado, redondeada a 2 decimales;
 *                     null cuando no hay punto de referencia (favoritos sin ubicación)
 * @param precio       el del carburante buscado; null si la búsqueda no filtró por uno
 * @param precios      todos los que vende, con la clave del carburante (GASOLINA_95_E5…)
 */
public record EstacionCercanaDto(
        String id,
        String rotulo,
        String direccion,
        String municipio,
        String provincia,
        String codigoPostal,
        String horario,
        double latitud,
        double longitud,
        Double distanciaKm,
        BigDecimal precio,
        Map<String, BigDecimal> precios
) {
}
