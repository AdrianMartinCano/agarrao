package dev.pimon.listillo.gasolina.estaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarburanteTest {

    @Test
    @DisplayName("El buscador solo ofrece los carburantes que la gente reposta")
    void habitualesSonLosDeSiempre() {
        assertThat(Carburante.habituales()).containsExactly(
                Carburante.GASOLINA_95_E5,
                Carburante.GASOLINA_98_E5,
                Carburante.GASOLEO_A,
                Carburante.GLP,
                Carburante.ADBLUE);
    }

    @Test
    @DisplayName("Los carburantes raros se siguen leyendo aunque no se ofrezcan")
    void losNoHabitualesSiguenExistiendo() {
        // No están en el desplegable, pero su precio se guarda igual y la API los acepta.
        // Si alguien los borrara del enum, dejarían de leerse los precios del Ministerio.
        assertThat(Carburante.values()).contains(
                Carburante.GASOLEO_PREMIUM,
                Carburante.BIODIESEL,
                Carburante.HIDROGENO);

        assertThat(Carburante.porCampoMinetur("Precio Gasoleo Premium"))
                .contains(Carburante.GASOLEO_PREMIUM);
    }

    @Test
    @DisplayName("Cada carburante tiene un nombre distinto para mostrar")
    void etiquetasSinDuplicados() {
        // Dos opciones con el mismo texto en el desplegable serían imposibles de
        // distinguir para quien las lee.
        List<String> etiquetas = Carburante.habituales().stream().map(Carburante::etiqueta).toList();

        assertThat(etiquetas).doesNotHaveDuplicates();
    }
}
