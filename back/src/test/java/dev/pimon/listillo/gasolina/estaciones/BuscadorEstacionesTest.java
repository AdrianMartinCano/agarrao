package dev.pimon.listillo.gasolina.estaciones;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasDto;
import dev.pimon.listillo.gasolina.estaciones.dto.BusquedaCercanasRequest;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import dev.pimon.listillo.gasolina.estaciones.dto.PaginaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Búsqueda por cercanía. Se prueba contra un catálogo de mentira (sin red y sin Spring),
 * que es lo que permite que estos tests corran en milisegundos.
 */
class BuscadorEstacionesTest {

    // Puerta del Sol, el punto desde el que se busca en todos los casos.
    private static final double LAT = 40.4168, LON = -3.7038;

    private CatalogoEstaciones catalogo;
    private BuscadorEstaciones buscador;

    @BeforeEach
    void setUp() {
        catalogo = mock(CatalogoEstaciones.class);
        when(catalogo.disponible()).thenReturn(true);

        var props = new GasolinaProperties(
                new GasolinaProperties.Minetur("http://ejemplo", java.time.Duration.ofMinutes(30), java.time.Duration.ofSeconds(60), true),
                new GasolinaProperties.Busqueda(5, 50, 100),
                new GasolinaProperties.Rutas("http://ejemplo", java.time.Duration.ofSeconds(10), 50)
        );
        buscador = new BuscadorEstaciones(catalogo, props);
    }

    @Test
    @DisplayName("Solo devuelve las estaciones dentro del radio pedido")
    void filtraPorRadio() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("cerca", 40.4180, -3.7050, Map.of(Carburante.GASOLINA_95_E5, "1.70")),
                estacion("lejos", 41.3874,  2.1686, Map.of(Carburante.GASOLINA_95_E5, "1.50"))   // Barcelona
        ));

        PaginaDto<EstacionCercanaDto> resultado = buscar(2.0, null, null, null, null);

        assertThat(resultado.contenido()).extracting(EstacionCercanaDto::id).containsExactly("cerca");
        assertThat(resultado.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("Con carburante, descarta las estaciones que no lo venden")
    void filtraPorCarburante() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("con-glp", 40.4180, -3.7050, Map.of(Carburante.GLP, "0.90")),
                estacion("sin-glp", 40.4181, -3.7051, Map.of(Carburante.GASOLINA_95_E5, "1.70"))
        ));

        PaginaDto<EstacionCercanaDto> resultado = buscar(5.0, Carburante.GLP, null, null, null);

        assertThat(resultado.contenido()).extracting(EstacionCercanaDto::id).containsExactly("con-glp");
    }

    @Test
    @DisplayName("Por defecto ordena de la más cercana a la más lejana")
    void ordenaPorDistanciaPorDefecto() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("media", 40.4300, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.60")),
                estacion("pegada", 40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.90")),
                estacion("lejana", 40.4500, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.50"))
        ));

        PaginaDto<EstacionCercanaDto> resultado = buscar(20.0, null, null, null, null);

        assertThat(resultado.contenido())
                .extracting(EstacionCercanaDto::id)
                .containsExactly("pegada", "media", "lejana");
    }

    @Test
    @DisplayName("Ordenando por precio, la más barata va primero y empata por distancia")
    void ordenaPorPrecio() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("cara",         40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.90")),
                estacion("barata-lejos", 40.4500, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.50")),
                estacion("barata-cerca", 40.4171, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.50"))
        ));

        PaginaDto<EstacionCercanaDto> resultado =
                buscar(20.0, Carburante.GASOLINA_95_E5, OrdenBusqueda.PRECIO, null, null);

        assertThat(resultado.contenido())
                .extracting(EstacionCercanaDto::id)
                .containsExactly("barata-cerca", "barata-lejos", "cara");
    }

    @Test
    @DisplayName("Las favoritas del usuario salen las primeras, aunque estén más lejos")
    void favoritasPrimero() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("cerca",    40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70")),
                estacion("favorita", 40.4500, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.90")),
                estacion("media",    40.4300, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.60"))
        ));

        var resultado = buscar(20.0, null, null, null, null, Set.of("favorita"));

        assertThat(resultado.contenido())
                .extracting(EstacionCercanaDto::id)
                .containsExactly("favorita", "cerca", "media");
    }

    @Test
    @DisplayName("Dentro de las favoritas se respeta el orden pedido")
    void favoritasEntreSiSiguenElOrden() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("fav-cara",   40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.90")),
                estacion("fav-barata", 40.4500, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.50")),
                estacion("normal",     40.4171, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.40"))
        ));

        var resultado = buscar(20.0, Carburante.GASOLINA_95_E5, OrdenBusqueda.PRECIO, null, null,
                Set.of("fav-cara", "fav-barata"));

        // Las dos favoritas arriba y entre ellas la más barata primero; la normal, aunque
        // sea la más barata de todas, va detrás.
        assertThat(resultado.contenido())
                .extracting(EstacionCercanaDto::id)
                .containsExactly("fav-barata", "fav-cara", "normal");
    }

    @Test
    @DisplayName("Una favorita fuera del radio o del carburante no se cuela en los resultados")
    void favoritaNoSaltaLosFiltros() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("fav-lejos", 41.3874,  2.1686, Map.of(Carburante.GASOLINA_95_E5, "1.50")), // Barcelona
                estacion("fav-sin-glp", 40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70")),
                estacion("con-glp",   40.4171, -3.7038, Map.of(Carburante.GLP, "0.90"))
        ));

        var resultado = buscar(5.0, Carburante.GLP, null, null, null,
                Set.of("fav-lejos", "fav-sin-glp"));

        // Ser favorita cambia el ORDEN, no lo que entra: los filtros mandan igual.
        assertThat(resultado.contenido()).extracting(EstacionCercanaDto::id).containsExactly("con-glp");
    }

    @Test
    @DisplayName("Ordenar por precio sin decir qué carburante es un error del cliente")
    void ordenPorPrecioSinCarburante() {
        assertThatThrownBy(() -> buscar(5.0, null, OrdenBusqueda.PRECIO, null, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("carburante");
    }

    @Test
    @DisplayName("Un radio desmedido se recorta al máximo configurado en vez de fallar")
    void radioSeRecortaAlMaximo() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("a-100-km", 41.3168, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70"))
        ));

        // Se piden 5.000 km (media Europa); el tope de configuración son 50.
        PaginaDto<EstacionCercanaDto> resultado = buscar(5000.0, null, null, null, null);

        assertThat(resultado.contenido()).isEmpty();
    }

    @Test
    @DisplayName("Sin datos del Ministerio responde 503, no 500")
    void sinCatalogoDevuelve503() {
        when(catalogo.disponible()).thenReturn(false);

        assertThatThrownBy(() -> buscar(5.0, null, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("La paginación parte los resultados y mantiene el orden")
    void pagina() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("1", 40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70")),
                estacion("2", 40.4180, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70")),
                estacion("3", 40.4190, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70"))
        ));

        PaginaDto<EstacionCercanaDto> segunda = buscar(20.0, null, null, 1, 2);

        assertThat(segunda.contenido()).extracting(EstacionCercanaDto::id).containsExactly("3");
        assertThat(segunda.total()).isEqualTo(3);
        assertThat(segunda.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Pedir una página que no existe devuelve vacío, no un error")
    void paginaFueraDeRango() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("1", 40.4170, -3.7038, Map.of(Carburante.GASOLINA_95_E5, "1.70"))
        ));

        PaginaDto<EstacionCercanaDto> pagina = buscar(20.0, null, null, 99, 20);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("La distancia devuelta viene redondeada a dos decimales")
    void distanciaRedondeada() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("x", 40.4300, -3.7100, Map.of(Carburante.GASOLINA_95_E5, "1.70"))
        ));

        Double distancia = buscar(20.0, null, null, null, null).contenido().get(0).distanciaKm();

        assertThat(distancia).isNotNull();
        assertThat(BigDecimal.valueOf(distancia).scale()).isLessThanOrEqualTo(2);
    }

    // ── El resumen de precios de la zona ── //

    @Test
    @DisplayName("El resumen promedia TODAS las encontradas, no solo la página que se ve")
    void resumenSobreElConjuntoEntero() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("1", 40.4180, -3.7050, Map.of(Carburante.GASOLINA_95_E5, "1.500")),
                estacion("2", 40.4185, -3.7055, Map.of(Carburante.GASOLINA_95_E5, "1.600")),
                estacion("3", 40.4190, -3.7060, Map.of(Carburante.GASOLINA_95_E5, "1.700"))
        ));

        // Se pide una página de UNA sola estación: si el resumen se calculara sobre lo
        // paginado, la media sería 1,500 y cambiaría al pasar de página.
        var resultado = busqueda(5.0, Carburante.GASOLINA_95_E5, OrdenBusqueda.PRECIO, 0, 1, Set.of());

        assertThat(resultado.estaciones().contenido()).hasSize(1);
        assertThat(resultado.resumen().medio()).isEqualByComparingTo("1.600");
        assertThat(resultado.resumen().minimo()).isEqualByComparingTo("1.500");
        assertThat(resultado.resumen().estaciones()).isEqualTo(3);
    }

    @Test
    @DisplayName("Sin carburante elegido no hay resumen que dar")
    void sinCarburanteNoHayResumen() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("1", 40.4180, -3.7050,
                        Map.of(Carburante.GASOLINA_95_E5, "1.500", Carburante.GASOLEO_A, "1.400"))
        ));

        // Cada estación vende varios carburantes a precios distintos: promediarlos todos
        // daría un número sin significado.
        assertThat(busqueda(5.0, null, null, null, null, Set.of()).resumen()).isNull();
    }

    @Test
    @DisplayName("Sin resultados tampoco hay resumen")
    void sinResultadosNoHayResumen() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("lejos", 41.0, -3.7, Map.of(Carburante.GASOLINA_95_E5, "1.500"))
        ));

        var resultado = busqueda(2.0, Carburante.GASOLINA_95_E5, null, null, null, Set.of());

        assertThat(resultado.estaciones().contenido()).isEmpty();
        assertThat(resultado.resumen()).isNull();
    }

    @Test
    @DisplayName("El resumen solo cuenta las del radio, como la lista")
    void resumenRespetaElRadio() {
        when(catalogo.todas()).thenReturn(List.of(
                estacion("cerca", 40.4180, -3.7050, Map.of(Carburante.GASOLINA_95_E5, "1.700")),
                // A 60 km: no sale en la lista, así que tampoco puede tirar de la media.
                estacion("lejos", 40.9500, -3.7050, Map.of(Carburante.GASOLINA_95_E5, "1.100"))
        ));

        var resultado = busqueda(5.0, Carburante.GASOLINA_95_E5, null, null, null, Set.of());

        assertThat(resultado.resumen().estaciones()).isEqualTo(1);
        assertThat(resultado.resumen().medio()).isEqualByComparingTo("1.700");
    }

    // ── apoyo ── //

    /** Solo la página: es lo que mira casi todo lo de aquí. Para el resumen, {@link #busqueda}. */
    private PaginaDto<EstacionCercanaDto> buscar(Double radioKm, Carburante carburante,
                                                 OrdenBusqueda orden, Integer pagina, Integer tamano) {
        return buscar(radioKm, carburante, orden, pagina, tamano, Set.of());
    }

    private PaginaDto<EstacionCercanaDto> buscar(Double radioKm, Carburante carburante,
                                                 OrdenBusqueda orden, Integer pagina, Integer tamano,
                                                 Set<String> idsFavoritos) {
        return busqueda(radioKm, carburante, orden, pagina, tamano, idsFavoritos).estaciones();
    }

    private BusquedaCercanasDto busqueda(Double radioKm, Carburante carburante,
                                         OrdenBusqueda orden, Integer pagina, Integer tamano,
                                         Set<String> idsFavoritos) {
        return buscador.cercanas(new BusquedaCercanasRequest(
                LAT, LON, radioKm, carburante, orden, pagina, tamano, idsFavoritos));
    }

    private static Estacion estacion(String id, double lat, double lon, Map<Carburante, String> precios) {
        Map<Carburante, BigDecimal> convertidos = precios.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> new BigDecimal(e.getValue())));

        return new Estacion(id, "RÓTULO", "Calle de prueba", "Madrid", "MADRID", "MADRID",
                "28001", "L-D: 24H", lat, lon, convertidos);
    }
}
