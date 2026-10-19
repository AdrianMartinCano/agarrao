package dev.pimon.listillo.gasolina.estaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.listillo.gasolina.estaciones.minetur.MineturRespuesta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deserialización del JSON del Ministerio.
 *
 * El JSON de ejemplo está copiado literal de una respuesta real, con sus claves raras
 * ("Longitud (WGS84)", "C.P."), sus decimales con coma y sus precios vacíos. Es el
 * contrato con un servicio externo que no controlamos: si cambia, estos tests son lo
 * que avisa.
 */
class MineturEstacionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String JSON_REAL = """
            {
              "Fecha": "25/08/2026 11:03:58",
              "ResultadoConsulta": "OK",
              "ListaEESSPrecio": [
                {
                  "C.P.": "05296",
                  "Dirección": "AUTOVIA NOROESTE KM. 111",
                  "Horario": "L-D: 24H",
                  "Latitud": "40,955111",
                  "Localidad": "ADANERO",
                  "Longitud (WGS84)": "-4,614556",
                  "Municipio": "Adanero",
                  "Precio Biodiesel": "",
                  "Precio Gasoleo A": "1,994",
                  "Precio Gasolina 95 E5": "1,779",
                  "Precio Gasolina 98 E5": "1,939",
                  "Precio Carburante Del Futuro": "9,999",
                  "Provincia": "ÁVILA",
                  "Rótulo": "CEPSA",
                  "IDEESS": "4982"
                }
              ]
            }
            """;

    @Test
    @DisplayName("Lee una estación real: coordenadas y precios con coma decimal")
    void deserializaEstacionReal() throws Exception {
        MineturRespuesta respuesta = mapper.readValue(JSON_REAL, MineturRespuesta.class);

        assertThat(respuesta.correcta()).isTrue();
        assertThat(respuesta.estaciones()).hasSize(1);

        Estacion estacion = respuesta.estaciones().get(0).aEstacion().orElseThrow();

        assertThat(estacion.id()).isEqualTo("4982");
        assertThat(estacion.rotulo()).isEqualTo("CEPSA");
        assertThat(estacion.municipio()).isEqualTo("Adanero");
        assertThat(estacion.codigoPostal()).isEqualTo("05296");
        assertThat(estacion.latitud()).isEqualTo(40.955111);
        assertThat(estacion.longitud()).isEqualTo(-4.614556);
        assertThat(estacion.precio(Carburante.GASOLINA_95_E5)).contains(new BigDecimal("1.779"));
        assertThat(estacion.precio(Carburante.GASOLEO_A)).contains(new BigDecimal("1.994"));
    }

    @Test
    @DisplayName("Un precio vacío significa que no se vende, no que valga cero")
    void precioVacioNoEsCero() throws Exception {
        MineturRespuesta respuesta = mapper.readValue(JSON_REAL, MineturRespuesta.class);
        Estacion estacion = respuesta.estaciones().get(0).aEstacion().orElseThrow();

        // Si el "" se colara como 0, esta estación aparecería siempre la primera al
        // ordenar por precio de biodiésel. Ha pasado en más de un comparador de precios.
        assertThat(estacion.vende(Carburante.BIODIESEL)).isFalse();
        assertThat(estacion.precio(Carburante.BIODIESEL)).isEmpty();
    }

    @Test
    @DisplayName("Un carburante que aún no conocemos se ignora sin romper la carga")
    void carburanteDesconocidoNoRompe() throws Exception {
        MineturRespuesta respuesta = mapper.readValue(JSON_REAL, MineturRespuesta.class);
        Estacion estacion = respuesta.estaciones().get(0).aEstacion().orElseThrow();

        // El JSON trae "Precio Carburante Del Futuro", que no está en el enum. Que el
        // Ministerio añada una columna no puede tumbar la descarga de las 11.000.
        assertThat(estacion.precios()).hasSize(3);
    }

    @Test
    @DisplayName("Se descarta la estación sin coordenadas usables")
    void descartaSinCoordenadas() throws Exception {
        String json = """
                {
                  "IDEESS": "1",
                  "Latitud": "",
                  "Longitud (WGS84)": "-4,61",
                  "Precio Gasolina 95 E5": "1,70"
                }
                """;

        Optional<Estacion> estacion = mapper
                .readValue(json, dev.pimon.listillo.gasolina.estaciones.minetur.MineturEstacion.class)
                .aEstacion();

        assertThat(estacion).isEmpty();
    }

    @Test
    @DisplayName("Se descarta la estación que no publica ningún precio")
    void descartaSinPrecios() throws Exception {
        String json = """
                {
                  "IDEESS": "2",
                  "Latitud": "40,95",
                  "Longitud (WGS84)": "-4,61",
                  "Precio Gasolina 95 E5": ""
                }
                """;

        Optional<Estacion> estacion = mapper
                .readValue(json, dev.pimon.listillo.gasolina.estaciones.minetur.MineturEstacion.class)
                .aEstacion();

        assertThat(estacion).isEmpty();
    }
}
