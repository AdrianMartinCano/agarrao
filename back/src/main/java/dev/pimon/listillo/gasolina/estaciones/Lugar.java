package dev.pimon.listillo.gasolina.estaciones;

/**
 * Un sitio desde el que se puede buscar: un municipio o un código postal.
 *
 * DE DÓNDE SALEN. No hay servicio de geocodificación detrás ni base de datos de
 * callejero: los lugares se sacan de las propias estaciones del Ministerio, que ya traen
 * municipio, provincia, código postal y coordenadas. Agrupando por municipio o por CP y
 * promediando sus coordenadas sale un punto perfectamente válido desde el que buscar.
 *
 * Por qué así y no con Nominatim o Geoapify:
 *   - no hay clave de API que gestionar, ni cuotas, ni límite de peticiones por segundo;
 *   - la búsqueda es instantánea, porque no sale del proceso;
 *   - y, sobre todo, solo se ofrecen sitios DONDE HAY GASOLINERAS. Un geocodificador de
 *     verdad te dejaría buscar en mitad de un embalse y devolver cero resultados.
 *
 * LA LIMITACIÓN, que conviene tener clara: esto no resuelve direcciones concretas.
 * "Calle Goya 24" no se puede encontrar; "28001" y "Getafe", sí. Para direcciones haría
 * falta un geocodificador externo de verdad.
 *
 * @param nombre      lo que se le enseña al usuario ("Getafe" o "28001")
 * @param detalle     dónde está, para desempatar entre municipios que se llaman igual
 * @param latitud     centro de las estaciones de ese lugar
 * @param estaciones  cuántas hay: sirve para poner los sitios grandes primero
 */
public record Lugar(
        String nombre,
        String detalle,
        TipoLugar tipo,
        double latitud,
        double longitud,
        int estaciones
) {

    public enum TipoLugar {
        MUNICIPIO,
        CODIGO_POSTAL
    }
}
