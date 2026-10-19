package dev.pimon.listillo.gasolina.estaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Todos los casos de esta clase están sacados de datos REALES del Ministerio, no
 * inventados: así es exactamente como llegan.
 */
class NombresLugaresTest {

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("El artículo pospuesto vuelve a su sitio")
    @CsvSource({
            "'Coruña (A)',                   'A Coruña'",
            "'Palmas de Gran Canaria (Las)', 'Las Palmas de Gran Canaria'",
            "'Vendrell (El)',                'El Vendrell'",
            "'Barcelona',                    'Barcelona'",
            "'Castellón de la Plana/Castelló de la Plana', 'Castellón de la Plana/Castelló de la Plana'"
    })
    void articuloPospuesto(String origen, String esperado) {
        assertThat(NombresLugares.municipio(origen)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("Las provincias dejan de venir gritando en mayúsculas")
    @CsvSource({
            "'ÁVILA',                'Ávila'",
            "'MADRID',               'Madrid'",
            "'CASTELLÓN / CASTELLÓ', 'Castellón / Castelló'",
            "'CORUÑA (A)',           'A Coruña'",
            "'PALMAS (LAS)',         'Las Palmas'",
            "'RIOJA (LA)',           'La Rioja'"
    })
    void provinciasEnMayusculas(String origen, String esperado) {
        assertThat(NombresLugares.provincia(origen)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Las preposiciones intermedias se quedan en minúscula")
    void preposicionesEnMinuscula() {
        assertThat(NombresLugares.provincia("SANTA CRUZ DE TENERIFE"))
                .isEqualTo("Santa Cruz de Tenerife");
    }

    @Test
    @DisplayName("Escribir el nombre bien encuentra la ciudad")
    void aCorunaEsEncontrable() {
        // Este es el motivo de existir de toda esta clase: con el nombre tal cual lo manda
        // el Ministerio ("Coruña (A)"), teclear "a coru" no encontraba nada.
        String nombre = NombresLugares.municipio("Coruña (A)");

        assertThat(IndiceLugares.normalizar(nombre)).startsWith("a coru");
    }
}
