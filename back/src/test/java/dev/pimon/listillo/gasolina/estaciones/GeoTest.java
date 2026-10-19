package dev.pimon.listillo.gasolina.estaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoTest {

    // Puerta del Sol (Madrid) y Plaza de Cataluña (Barcelona): ~505 km en línea recta.
    private static final double MADRID_LAT = 40.4168, MADRID_LON = -3.7038;
    private static final double BARNA_LAT  = 41.3874, BARNA_LON  =  2.1686;

    @Test
    @DisplayName("La distancia entre dos ciudades conocidas cuadra con la real")
    void distanciaEntreCiudades() {
        double km = Geo.distanciaKm(MADRID_LAT, MADRID_LON, BARNA_LAT, BARNA_LON);

        assertThat(km).isCloseTo(505, org.assertj.core.data.Offset.offset(5.0));
    }

    @Test
    @DisplayName("La distancia de un punto a sí mismo es cero")
    void distanciaCero() {
        assertThat(Geo.distanciaKm(MADRID_LAT, MADRID_LON, MADRID_LAT, MADRID_LON)).isZero();
    }

    @Test
    @DisplayName("La caja envuelve al círculo: nunca descarta un punto que sí estaba dentro del radio")
    void cajaNoDescartaPuntosValidos() {
        double radioKm = 10;
        Geo.CajaBusqueda caja = Geo.caja(MADRID_LAT, MADRID_LON, radioKm);

        // Se recorre el círculo entero: si en algún ángulo la caja se quedara corta, la
        // búsqueda perdería estaciones en silencio, que es el fallo difícil de detectar.
        for (int grados = 0; grados < 360; grados += 5) {
            double rad = Math.toRadians(grados);
            // Punto justo en el borde del radio, en la dirección de ese ángulo.
            double lat = MADRID_LAT + (radioKm / 111.32) * Math.cos(rad);
            double lon = MADRID_LON + (radioKm / (111.32 * Math.cos(Math.toRadians(MADRID_LAT)))) * Math.sin(rad);

            assertThat(caja.contiene(lat, lon))
                    .as("punto del borde a %d grados", grados)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("La caja sí descarta lo que está claramente lejos")
    void cajaDescartaLejanos() {
        Geo.CajaBusqueda caja = Geo.caja(MADRID_LAT, MADRID_LON, 10);

        assertThat(caja.contiene(BARNA_LAT, BARNA_LON)).isFalse();
    }

    @Test
    @DisplayName("Cerca del polo la caja no se rompe: acota en latitud y abre en longitud")
    void cajaEnElPolo() {
        Geo.CajaBusqueda caja = Geo.caja(89.999, 0, 10);

        // Junto al polo, un grado de longitud mide casi nada y el cálculo se dispara.
        // Se abre la caja en longitud a propósito: filtrar de más es correcto (el radio
        // real lo decide después el haversine), quedarse corto no lo sería.
        assertThat(caja.contiene(89.999, 179.0)).isTrue();
        assertThat(caja.contiene(0.0, 0.0)).isFalse();
    }
}
