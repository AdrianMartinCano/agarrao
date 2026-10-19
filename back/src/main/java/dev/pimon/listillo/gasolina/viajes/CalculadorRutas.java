package dev.pimon.listillo.gasolina.viajes;

import java.util.Optional;

/**
 * Quien sabe ir de un sitio a otro en coche. Es una interfaz para que los tests no salgan
 * a internet: la de verdad es {@link OsrmRutas}.
 */
public interface CalculadorRutas {

    /**
     * @return la ruta, o vacío si no hay camino en coche entre los dos puntos
     * @throws RutasNoDisponiblesException si el servicio de rutas no responde
     */
    Optional<Ruta> calcular(double origenLat, double origenLon, double destinoLat, double destinoLon);
}
