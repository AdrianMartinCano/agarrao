package dev.pimon.listillo.gasolina.estaciones;

/**
 * Cálculos sobre coordenadas. Sin estado, solo funciones.
 *
 * No usamos PostGIS ni una base geoespacial porque el catálogo entero son ~11.000
 * estaciones en memoria: recorrerlas es cuestión de microsegundos. Si algún día hubiera
 * que escalar, el reemplazo natural sería un índice espacial, no optimizar esto.
 */
public final class Geo {

    /** Radio medio de la Tierra en kilómetros. */
    private static final double RADIO_TIERRA_KM = 6371.0088;

    /** Kilómetros que mide un grado de latitud (constante en toda la Tierra). */
    private static final double KM_POR_GRADO_LATITUD = 111.32;

    private Geo() {
    }

    /**
     * Distancia en km sobre la superficie entre dos puntos (fórmula del haversine).
     *
     * Es la distancia en línea recta, no la de carretera: para ordenar gasolineras
     * cercanas basta y no depende de ningún servicio externo. La distancia real
     * conduciendo requeriría una API de rutas, y eso es otra historia.
     */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Caja rectangular que envuelve al círculo de radio {@code radioKm} alrededor del punto.
     *
     * Sirve de descarte rápido antes del haversine: comparar cuatro números es más barato
     * que un par de senos y un arcotangente, y tira el 99% de las estaciones. La caja es
     * MÁS GRANDE que el círculo, así que nunca descarta una estación que sí entraba; el
     * radio exacto lo decide después la distancia real.
     */
    public static CajaBusqueda caja(double latitud, double longitud, double radioKm) {
        double deltaLat = radioKm / KM_POR_GRADO_LATITUD;

        // Los meridianos se juntan según subes de latitud: un grado de longitud mide
        // menos km en Bilbao que en Cádiz. Sin el coseno, la caja se quedaría corta por
        // los lados en el norte y perdería estaciones que sí estaban dentro del radio.
        double kmPorGradoLongitud = KM_POR_GRADO_LATITUD * Math.cos(Math.toRadians(latitud));
        double deltaLon = Math.abs(kmPorGradoLongitud) < 0.001
                ? 180.0                                  // en los polos no acotamos por longitud
                : radioKm / kmPorGradoLongitud;

        return new CajaBusqueda(
                latitud  - deltaLat, latitud  + deltaLat,
                longitud - Math.abs(deltaLon), longitud + Math.abs(deltaLon)
        );
    }

    public record CajaBusqueda(double latMin, double latMax, double lonMin, double lonMax) {
        public boolean contiene(double latitud, double longitud) {
            return latitud  >= latMin && latitud  <= latMax
                && longitud >= lonMin && longitud <= lonMax;
        }
    }
}
