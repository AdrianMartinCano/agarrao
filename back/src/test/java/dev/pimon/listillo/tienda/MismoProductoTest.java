package dev.pimon.listillo.tienda;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Reconocer el mismo producto en dos súper por su nombre y su tamaño. */
class MismoProductoTest {

    /** Pesos como los reales: la marca es rara, «zero» o «azúcar» salen en muchos productos. */
    private static final Map<String, Double> PESOS = Map.of(
            "coca", 6.0, "pepsi", 6.5, "cola", 4.0, "zero", 4.0, "azucar", 2.0, "cafeina", 6.0,
            "nutella", 7.0, "crema", 3.0, "cacao", 3.0, "avellana", 4.0);

    private static double parecido(String uno, String otro) {
        return BuscadorService.parecido(BuscadorService.significativas(uno), BuscadorService.significativas(otro),
                p -> PESOS.getOrDefault(p, 5.0));
    }

    @Test
    void sinEnvasesNiCifrasYEnSingular() {
        assertThat(BuscadorService.significativas("Refresco cola Coca-Cola 2l pack botella 2 zero azúcar"))
                .containsExactly("cola", "coca", "zero", "azucar");
        assertThat(BuscadorService.significativas("Crema de cacao y avellanas Nutella 750g"))
                .containsExactly("crema", "cacao", "avellana", "nutella");
    }

    @Test
    void laMarcaManda() {
        String mercadona = "Refresco Coca-Cola zero azúcar zero cafeína";
        assertThat(parecido(mercadona, "Refresco cola Coca-Cola 2l pack botella 2 zero azúcar zero cafeína"))
                .isGreaterThanOrEqualTo(BuscadorService.PARECIDO_MINIMO);
        assertThat(parecido(mercadona, "Refresco de cola PEPSI ZERO AZÚCAR ZERO CAFEÍNA, lata 33 cl"))
                .isLessThan(BuscadorService.PARECIDO_MINIMO);
        assertThat(parecido("Crema Untar Avellana y Cacao Tarro Nutella", "Crema al cacao con avellanas Nutella"))
                .isGreaterThanOrEqualTo(BuscadorService.PARECIDO_MINIMO);
    }

    @Test
    void elMismoTamanoConMargen() {
        assertThat(BuscadorService.mismoTamano(new BigDecimal("1.000"), new BigDecimal("1.050"))).isTrue();
        assertThat(BuscadorService.mismoTamano(new BigDecimal("0.750"), new BigDecimal("0.450"))).isFalse();
        assertThat(List.of(BuscadorService.mismoTamano(new BigDecimal("2"), new BigDecimal("2")))).containsOnly(true);
    }
}
