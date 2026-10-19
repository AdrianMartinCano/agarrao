package dev.pimon.listillo.gasolina.viajes;

import java.util.List;

/**
 * Una ruta en coche ya calculada.
 *
 * @param puntos  la línea de la ruta, en orden, como {@code [latitud, longitud]}
 */
public record Ruta(double distanciaKm, double duracionMin, List<double[]> puntos) {
}
