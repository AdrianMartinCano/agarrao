package dev.pimon.listillo.gasolina.viajes;

import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;

import java.math.BigDecimal;
import java.util.List;

/** Lo que devuelve Viajes. */
public final class ViajeDtos {

    private ViajeDtos() {
    }

    /**
     * @param ruta        la línea para pintarla, {@code [latitud, longitud]}, ya aligerada
     * @param enRuta      cuántas gasolineras de camino venden ese carburante (todas, aunque
     *                    {@code estaciones} traiga solo las más baratas)
     * @param mediaRuta   el precio medio de esas gasolineras: la vara con la que medir
     * @param estaciones  de la más barata a la más cara
     */
    public record ViajeDto(
            double distanciaKm,
            long duracionMin,
            double desvioKm,
            List<double[]> ruta,
            int enRuta,
            BigDecimal mediaRuta,
            BigDecimal minimaRuta,
            List<GasolineraEnRutaDto> estaciones
    ) {
    }

    /**
     * @param kmDesdeOrigen  en qué kilómetro del viaje queda
     * @param desvioKm       a cuánto está de la carretera, en línea recta
     */
    public record GasolineraEnRutaDto(EstacionCercanaDto estacion, double kmDesdeOrigen, double desvioKm) {
    }
}
