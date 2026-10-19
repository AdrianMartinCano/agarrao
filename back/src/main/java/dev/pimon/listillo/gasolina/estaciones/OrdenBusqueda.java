package dev.pimon.listillo.gasolina.estaciones;

/** Criterio de ordenación de los resultados. */
public enum OrdenBusqueda {

    /** De la más cerca a la más lejos. Es el orden por defecto. */
    DISTANCIA,

    /**
     * De la más barata a la más cara. Exige haber filtrado por un carburante:
     * sin saber cuál, "más barata" no significa nada.
     */
    PRECIO
}
