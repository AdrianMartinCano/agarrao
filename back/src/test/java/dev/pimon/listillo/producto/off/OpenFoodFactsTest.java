package dev.pimon.listillo.producto.off;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenFoodFactsTest {

    @Test
    @DisplayName("quita el signo de cantidad estimada y se queda con la primera marca")
    void limpia() {
        var datos = new OpenFoodFacts.Producto("Nutella", null, "Nutella, Ferrero", "400 g e",
                List.of("en:spreads"), null).aDatos().orElseThrow();

        assertThat(datos.cantidad()).isEqualTo("400 g");
        assertThat(datos.marca()).isEqualTo("Nutella");
        assertThat(OpenFoodFacts.Producto.limpiarCantidad("1 L ℮")).isEqualTo("1 L");
        assertThat(OpenFoodFacts.Producto.limpiarCantidad("6 x 125 g")).isEqualTo("6 x 125 g");
    }

    @Test
    @DisplayName("sin nombre no sirve para la lista: cuenta como desconocido")
    void sinNombre() {
        assertThat(new OpenFoodFacts.Producto(" ", null, "Marca", "1 L", null, null).aDatos()).isEmpty();
    }
}
