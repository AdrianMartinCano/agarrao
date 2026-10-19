package dev.pimon.listillo.gasolina.repostajes;

import dev.pimon.listillo.gasolina.estaciones.Carburante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraConsumoTest {

    @Test
    @DisplayName("Entre dos llenados, el consumo sale de los litros del segundo")
    void tramoSencillo() {
        // 700 km con 47 litros = 6,7 l/100 km
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                lleno("2026-03-15", 10_700, "47")
        );

        assertThat(CalculadoraConsumo.consumoMedio(repostajes)).isEqualByComparingTo("6.7");
    }

    @Test
    @DisplayName("Un repostaje parcial NO se tira: sus litros cuentan en el tramo")
    void parcialSeAcumula() {
        // Lunes lleno · miércoles 12 L sin llenar · sábado lleno con 35 L.
        // 700 km y 12 + 35 = 47 litros → 6,7 l/100 km, igual que si hubiera sido de una vez.
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                parcial("2026-03-03", 10_300, "12"),
                lleno("2026-03-08", 10_700, "35")
        );

        assertThat(CalculadoraConsumo.consumoMedio(repostajes)).isEqualByComparingTo("6.7");
        // Un solo tramo: el parcial no abre ni cierra ninguno.
        assertThat(CalculadoraConsumo.tramos(repostajes)).hasSize(1);
    }

    @Test
    @DisplayName("Un parcial suelto no inventa un consumo absurdo")
    void parcialSoloNoDaConsumo() {
        // Es EL caso que motiva todo esto: 12 litros en 500 km daría 2,4 l/100 km.
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                parcial("2026-03-05", 10_500, "12")
        );

        // El tramo sigue abierto: hasta el próximo lleno no se sabe cuánto se ha gastado.
        assertThat(CalculadoraConsumo.tramos(repostajes)).isEmpty();
        assertThat(CalculadoraConsumo.consumoMedio(repostajes)).isNull();
    }

    @Test
    @DisplayName("Con un solo repostaje todavía no hay nada que medir")
    void unSoloRepostaje() {
        assertThat(CalculadoraConsumo.consumoMedio(List.of(lleno("2026-03-01", 10_000, "40"))))
                .isNull();
    }

    @Test
    @DisplayName("Los litros del primer llenado no cuentan para su propio tramo")
    void litrosDelPrimerLlenoNoCuentan() {
        // Esos 40 litros pagaron los kilómetros ANTERIORES, no los del tramo que abren.
        // Si se sumaran, el consumo saldría casi al doble.
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                lleno("2026-03-15", 10_700, "47")
        );

        assertThat(CalculadoraConsumo.tramos(repostajes))
                .singleElement()
                .extracting(CalculadoraConsumo.Tramo::litros)
                .isEqualTo(new BigDecimal("47"));
    }

    @Test
    @DisplayName("Sin kilómetros no se mide, pero los litros no se pierden")
    void sinKilometros() {
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                lleno("2026-03-10", null, "20"),      // se olvidó apuntar el cuadro
                lleno("2026-03-20", 10_900, "45")
        );

        // El tramo medible es el del 10 al 20 de marzo... pero al no tener kilómetros el
        // del día 10, ese tramo no se puede medir. Y los 20 litros ya se habían cerrado
        // con él, así que el resultado es que no hay ningún tramo completo.
        assertThat(CalculadoraConsumo.tramos(repostajes)).isEmpty();
    }

    @Test
    @DisplayName("Si el cuentakilómetros retrocede, ese tramo se descarta")
    void kilometrosQueRetroceden() {
        // Una errata al teclear, o un repostaje que en realidad era de otro coche.
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                lleno("2026-03-15", 9_500, "47")
        );

        assertThat(CalculadoraConsumo.tramos(repostajes)).isEmpty();
    }

    @Test
    @DisplayName("La media pondera por kilómetros, no promedia las medias")
    void mediaPonderada() {
        var repostajes = List.of(
                lleno("2026-03-01", 10_000, "40"),
                lleno("2026-03-15", 11_000, "50"),   // 1.000 km, 50 L → 5,0
                lleno("2026-03-16", 11_100, "10")    // 100 km, 10 L → 10,0
        );

        // Promediar las medias daría (5,0 + 10,0) / 2 = 7,5, dándole al trayecto corto de
        // ciudad el mismo peso que a los 1.000 km de carretera.
        // Lo correcto: 60 litros en 1.100 km = 5,5.
        assertThat(CalculadoraConsumo.consumoMedio(repostajes)).isEqualByComparingTo("5.5");
    }

    @Test
    @DisplayName("El orden en que se guardaron los repostajes da igual")
    void ordenIndiferente() {
        var desordenados = List.of(
                lleno("2026-03-08", 10_700, "35"),
                lleno("2026-03-01", 10_000, "40"),
                parcial("2026-03-03", 10_300, "12")
        );

        assertThat(CalculadoraConsumo.consumoMedio(desordenados)).isEqualByComparingTo("6.7");
    }

    // ── apoyo ── //

    private static Repostaje lleno(String fecha, Integer km, String litros) {
        return crear(fecha, km, litros, true);
    }

    private static Repostaje parcial(String fecha, Integer km, String litros) {
        return crear(fecha, km, litros, false);
    }

    private static Repostaje crear(String fecha, Integer km, String litros, boolean depositoLleno) {
        Repostaje repostaje = new Repostaje();
        repostaje.setFecha(LocalDate.parse(fecha));
        repostaje.setKilometros(km);
        repostaje.setLitros(new BigDecimal(litros));
        repostaje.setDepositoLleno(depositoLleno);
        repostaje.setCarburante(Carburante.GASOLINA_95_E5);
        repostaje.setPrecioLitro(new BigDecimal("1.700"));
        repostaje.setImporte(new BigDecimal("50.00"));
        return repostaje;
    }
}
