package dev.pimon.listillo.gasolina.estaciones.dto;

import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.OrdenBusqueda;

import java.util.Set;

/**
 * Parámetros de una búsqueda de estaciones cercanas.
 *
 * No lleva anotaciones de Bean Validation a propósito: los rangos (latitud, longitud,
 * radio…) se validan en {@code EstacionesController}, sobre los propios query params.
 * Este record es el contrato con el servicio, y llega aquí ya saneado.
 *
 * Los campos opcionales admiten null y el servicio decide el valor por defecto, que
 * sale de la configuración y no de una constante repartida por el código.
 *
 * @param carburante    opcional: si viene, solo salen estaciones que lo venden
 * @param radioKm       opcional: si no viene, se usa el de configuración
 * @param orden         opcional: por defecto, por distancia
 * @param idsFavoritos  favoritas del usuario, para ponerlas las primeras. Vacío si la
 *                      búsqueda es anónima. NO llega en la petición HTTP: lo rellena el
 *                      controller a partir del token, porque si viniera por parámetro
 *                      cualquiera podría preguntar por los favoritos de otro.
 */
public record BusquedaCercanasRequest(
        double latitud,
        double longitud,
        Double radioKm,
        Carburante carburante,
        OrdenBusqueda orden,
        Integer pagina,
        Integer tamano,
        Set<String> idsFavoritos
) {

    /** Normaliza el conjunto para que el buscador nunca tenga que comprobar si es null. */
    public BusquedaCercanasRequest {
        idsFavoritos = idsFavoritos == null ? Set.of() : Set.copyOf(idsFavoritos);
    }
}
