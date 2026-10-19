package dev.pimon.listillo.gasolina.viajes;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.GasolinaProperties;
import dev.pimon.listillo.gasolina.viajes.ViajeDtos.GasolineraEnRutaDto;
import dev.pimon.listillo.gasolina.viajes.ViajeDtos.ViajeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GasolinerasEnRutaTest {

    /** Una carretera recta de 111 km hacia el norte, de (40, −3.7) a (41, −3.7). */
    private static final Ruta RECTA = new Ruta(111.3, 70, tramos(40.0, 41.0, -3.7, 50));
    /** Un km en longitud a la latitud 40,3. */
    private static final double UN_KM_ESTE = 1 / (111.32 * Math.cos(Math.toRadians(40.3)));

    private CalculadorRutas rutas;
    private CatalogoEstaciones catalogo;
    private GasolinerasEnRuta servicio;

    @BeforeEach
    void setUp() {
        rutas = mock(CalculadorRutas.class);
        catalogo = mock(CatalogoEstaciones.class);
        var props = new GasolinaProperties(
                new GasolinaProperties.Minetur("http://ejemplo", Duration.ofMinutes(30), Duration.ofSeconds(60), false),
                new GasolinaProperties.Busqueda(5, 50, 100),
                new GasolinaProperties.Rutas("http://ejemplo", Duration.ofSeconds(10), 50));
        servicio = new GasolinerasEnRuta(rutas, catalogo, props);
    }

    @Test
    @DisplayName("Solo las de camino y con ese carburante, de la más barata a la más cara")
    void deCaminoOrdenadas() {
        when(rutas.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Optional.of(RECTA));
        when(catalogo.todas()).thenReturn(List.of(
                estacion("en-la-carretera", 40.5, -3.7, "1.600"),
                estacion("a-un-km", 40.3, -3.7 + UN_KM_ESTE, "1.500"),
                estacion("a-cinco-km", 40.3, -3.7 + 5 * UN_KM_ESTE, "1.300"),
                estacion("pasado-el-destino", 41.2, -3.7, "1.200"),
                new Estacion("sin-gasolina", "X", "C", "M", "M", "P", "28001", "24H", 40.6, -3.7,
                        Map.of(Carburante.GASOLEO_A, new BigDecimal("1.100")))));

        ViajeDto viaje = servicio.buscar(40.0, -3.7, 41.0, -3.7, Carburante.GASOLINA_95_E5, null);

        assertThat(viaje.estaciones()).extracting(g -> g.estacion().id())
                .containsExactly("a-un-km", "en-la-carretera");
        GasolineraEnRutaDto primera = viaje.estaciones().get(0);
        assertThat(primera.desvioKm()).isCloseTo(1.0, within(0.05));
        // 0,3 grados al norte del origen ≈ 33,4 km de viaje
        assertThat(primera.kmDesdeOrigen()).isCloseTo(33.4, within(0.3));
        assertThat(viaje.enRuta()).isEqualTo(2);
        assertThat(viaje.mediaRuta()).isEqualByComparingTo("1.550");
        assertThat(viaje.minimaRuta()).isEqualByComparingTo("1.500");
        assertThat(viaje.desvioKm()).isEqualTo(2);
    }

    @Test
    @DisplayName("Con más desvío entran las que quedan más lejos de la carretera")
    void masDesvio() {
        when(rutas.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Optional.of(RECTA));
        when(catalogo.todas()).thenReturn(List.of(estacion("a-cinco-km", 40.3, -3.7 + 5 * UN_KM_ESTE, "1.300")));

        assertThat(servicio.buscar(40.0, -3.7, 41.0, -3.7, Carburante.GASOLINA_95_E5, 6.0).estaciones()).hasSize(1);
    }

    @Test
    @DisplayName("Sin camino en coche: 404 con un mensaje que se entiende")
    void sinRuta() {
        when(rutas.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscar(40.0, -3.7, 39.5, 2.6, Carburante.GASOLINA_95_E5, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Si el servicio de rutas no contesta: 503, no un 500")
    void rutasCaidas() {
        when(rutas.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RutasNoDisponiblesException("caído", null));

        assertThatThrownBy(() -> servicio.buscar(40.0, -3.7, 41.0, -3.7, Carburante.GASOLINA_95_E5, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Origen y destino en el mismo sitio: 400 sin llamar a nadie")
    void mismoSitio() {
        assertThatThrownBy(() -> servicio.buscar(40.0, -3.7, 40.001, -3.7, Carburante.GASOLINA_95_E5, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("La ruta para pintar se aligera, pero empieza y acaba donde la de verdad")
    void rutaAligerada() {
        List<double[]> muchos = tramos(40.0, 41.0, -3.7, 5000);

        List<double[]> pocos = GasolinerasEnRuta.paraPintar(muchos);

        assertThat(pocos.size()).isLessThanOrEqualTo(GasolinerasEnRuta.PUNTOS_PARA_PINTAR + 1);
        assertThat(pocos.get(0)).isEqualTo(muchos.get(0));
        assertThat(pocos.get(pocos.size() - 1)).isEqualTo(muchos.get(muchos.size() - 1));
    }

    @Test
    @DisplayName("Lee la respuesta de OSRM, que viene en longitud,latitud")
    void leeOsrm() throws Exception {
        var json = new ObjectMapper().readTree("""
                {"code":"Ok","routes":[{"distance":355000.5,"duration":12600,
                 "geometry":{"type":"LineString","coordinates":[[-3.70,40.41],[-0.37,39.47]]}}]}
                """);

        Ruta ruta = OsrmRutas.leer(json).orElseThrow();

        assertThat(ruta.distanciaKm()).isCloseTo(355.0, within(0.01));
        assertThat(ruta.duracionMin()).isEqualTo(210);
        assertThat(ruta.puntos().get(0)).containsExactly(40.41, -3.70);
    }

    @Test
    @DisplayName("OSRM sin camino (NoRoute) no es un fallo: es «no hay ruta»")
    void osrmSinRuta() throws Exception {
        var json = new ObjectMapper().readTree("{\"code\":\"NoRoute\",\"message\":\"Impossible route\"}");

        assertThat(OsrmRutas.leer(json)).isEmpty();
    }

    private static List<double[]> tramos(double latDesde, double latHasta, double lon, int n) {
        List<double[]> puntos = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            puntos.add(new double[]{latDesde + (latHasta - latDesde) * i / n, lon});
        }
        return puntos;
    }

    private static Estacion estacion(String id, double lat, double lon, String precio) {
        return new Estacion(id, "ROTULO " + id, "Calle " + id, "Municipio", "Municipio", "MADRID",
                "28001", "L-D: 24H", lat, lon, Map.of(Carburante.GASOLINA_95_E5, new BigDecimal(precio)));
    }
}
