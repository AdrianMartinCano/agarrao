package dev.pimon.listillo.gasolina.estaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class IndiceLugaresTest {

    private static final IndiceLugares INDICE = IndiceLugares.de(List.of(
            estacion("1", "Getafe",           "MADRID", "28901", 40.30, -3.73),
            estacion("2", "Getafe",           "MADRID", "28903", 40.32, -3.75),
            estacion("3", "Alcalá de Henares", "MADRID", "28801", 40.48, -3.36),
            estacion("4", "Madrid",           "MADRID", "28001", 40.42, -3.70),
            estacion("5", "Madrid",           "MADRID", "28001", 40.44, -3.68),
            estacion("6", "Madrid",           "MADRID", "28001", 40.40, -3.72),
            // Mismo nombre de municipio en otra provincia: el caso que rompe un índice
            // que agrupara solo por nombre.
            estacion("7", "Getafe",           "TOLEDO", "45001", 39.86, -4.02)
    ));

    @Test
    @DisplayName("Encuentra un municipio escribiendo su nombre")
    void buscaMunicipio() {
        assertThat(INDICE.buscar("getafe"))
                .extracting(Lugar::nombre)
                .contains("Getafe");
    }

    @Test
    @DisplayName("Encuentra sin escribir las tildes")
    void ignoraTildes() {
        // Nadie escribe "Alcalá" con tilde en un buscador.
        assertThat(INDICE.buscar("alcala"))
                .extracting(Lugar::nombre)
                .containsExactly("Alcalá de Henares");
    }

    @Test
    @DisplayName("Encuentra un código postal")
    void buscaCodigoPostal() {
        assertThat(INDICE.buscar("28001"))
                .extracting(Lugar::nombre)
                .contains("28001");
    }

    @Test
    @DisplayName("Dos municipios con el mismo nombre no se mezclan en un punto intermedio")
    void municipiosHomonimosSeparados() {
        List<Lugar> getafes = INDICE.buscar("getafe").stream()
                .filter(l -> l.tipo() == Lugar.TipoLugar.MUNICIPIO)
                .toList();

        // El de Madrid y el de Toledo son sitios distintos. Si se agruparan solo por
        // nombre, el punto medio caería en medio del campo, entre las dos provincias.
        assertThat(getafes).hasSize(2);
        assertThat(getafes).extracting(Lugar::detalle).containsExactlyInAnyOrder("Madrid", "Toledo");
    }

    @Test
    @DisplayName("La coordenada del lugar es el centro de sus gasolineras")
    void coordenadaEsElCentro() {
        Lugar madrid = INDICE.buscar("madrid").stream()
                .filter(l -> l.tipo() == Lugar.TipoLugar.MUNICIPIO)
                .findFirst()
                .orElseThrow();

        // Media de 40.42, 40.44 y 40.40
        assertThat(madrid.latitud()).isCloseTo(40.42, within(0.001));
        assertThat(madrid.longitud()).isCloseTo(-3.70, within(0.001));
        assertThat(madrid.estaciones()).isEqualTo(3);
    }

    @Test
    @DisplayName("Los sitios que empiezan por lo escrito van antes que los que solo lo contienen")
    void prefijosPrimero() {
        IndiceLugares indice = IndiceLugares.de(List.of(
                estacion("1", "Puente de Getafe", "MADRID", "28900", 40.30, -3.73),
                estacion("2", "Getafe",           "MADRID", "28901", 40.31, -3.74)
        ));

        assertThat(indice.buscar("getafe"))
                .extracting(Lugar::nombre)
                .startsWith("Getafe");
    }

    @Test
    @DisplayName("Con una sola letra no sugiere nada")
    void consultaDemasiadoCorta() {
        // Con "m" saldrían cientos de sitios y ninguno sería el que se busca.
        assertThat(INDICE.buscar("m")).isEmpty();
        assertThat(INDICE.buscar("")).isEmpty();
    }

    @Test
    @DisplayName("Un código postal mal formado no entra en el índice")
    void codigoPostalInvalido() {
        IndiceLugares indice = IndiceLugares.de(List.of(
                estacion("1", "Sitio", "MADRID", "", 40.0, -3.0),
                estacion("2", "Sitio", "MADRID", "2890", 40.0, -3.0)
        ));

        assertThat(indice.buscar("2890")).isEmpty();
    }

    private static Estacion estacion(String id, String municipio, String provincia,
                                     String codigoPostal, double lat, double lon) {
        return new Estacion(id, "REPSOL", "Calle de prueba", municipio, municipio, provincia,
                codigoPostal, "L-D: 24H", lat, lon,
                Map.of(Carburante.GASOLINA_95_E5, new BigDecimal("1.70")));
    }
}
