package dev.pimon.listillo.gasolina.estaciones.dto;

/**
 * Una sugerencia del buscador de lugares.
 *
 * @param nombre     "Getafe" o "28001"
 * @param detalle    dónde está, para distinguir municipios homónimos ("Madrid")
 * @param tipo       MUNICIPIO o CODIGO_POSTAL, para pintar un icono distinto
 * @param estaciones cuántas gasolineras hay: ayuda a elegir entre dos sitios parecidos
 */
public record LugarDto(
        String nombre,
        String detalle,
        String tipo,
        double latitud,
        double longitud,
        int estaciones
) {
}
