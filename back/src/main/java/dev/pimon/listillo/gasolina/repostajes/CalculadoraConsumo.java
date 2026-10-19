package dev.pimon.listillo.gasolina.repostajes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Consumo real (l/100 km) a partir del historial de repostajes.
 *
 * ═══ POR QUÉ NO ES UNA SIMPLE DIVISIÓN ═══
 *
 * La tentación es dividir los litros de un repostaje entre los kilómetros recorridos
 * desde el anterior. Con un ejemplo se ve por qué no sale:
 *
 *   Lunes    lleno el depósito              km 10.000
 *   Viernes  echo 20 € ≈ 12 litros, sin llenar   km 10.500
 *
 *   12 litros ÷ 500 km × 100 = 2,4 l/100 km  ← imposible
 *
 * En esos 500 km se gastaron unos 35 litros, pero solo se repusieron 12. Los otros 23
 * salieron de lo que ya había en el depósito. LOS LITROS QUE ECHAS NO SON LOS QUE
 * GASTASTE, salvo que el depósito quede al mismo nivel que la vez anterior. Y el único
 * nivel repetible con precisión es "lleno", porque el surtidor corta solo.
 *
 * ═══ CÓMO SE HACE ═══
 *
 * Se mide entre dos llenados completos, sumando TODO lo repostado por el camino:
 *
 *   Lunes     lleno            km 10.000
 *   Miércoles parcial  12 L    km 10.300
 *   Sábado    lleno    35 L    km 10.700
 *
 *   700 km recorridos, 12 + 35 = 47 litros  →  6,7 l/100 km  ✓
 *
 * Así los repostajes parciales no se pierden: no dan un punto de consumo por sí mismos,
 * pero sus litros cuentan en el tramo que los engloba. Se puede echar 20 € cuando haga
 * falta sin estropear las estadísticas.
 */
final class CalculadoraConsumo {

    private CalculadoraConsumo() {
    }

    /**
     * Un tramo entre dos llenados completos.
     *
     * @param kilometros  recorridos entre los dos llenados
     * @param litros      todo lo repostado en ese tramo, parciales incluidos
     * @param consumo     litros a los 100 km
     */
    record Tramo(int kilometros, BigDecimal litros, BigDecimal consumo, java.time.LocalDate hasta) {
    }

    /**
     * Calcula un tramo por cada par de llenados completos consecutivos.
     *
     * @param repostajes de UN solo vehículo; el orden da igual, se ordenan aquí
     * @return lista en orden cronológico; vacía si aún no hay dos llenados con kilómetros
     */
    static List<Tramo> tramos(List<Repostaje> repostajes) {
        // Por fecha y, a igualdad, por kilómetros: dos repostajes el mismo día ocurrieron
        // en el orden en que subió el cuentakilómetros.
        List<Repostaje> ordenados = repostajes.stream()
                .sorted(Comparator.comparing(Repostaje::getFecha)
                        .thenComparing(r -> r.getKilometros() == null ? 0 : r.getKilometros()))
                .toList();

        List<Tramo> tramos = new ArrayList<>();

        Repostaje llenoAnterior = null;
        // Litros acumulados desde el último lleno, parciales incluidos.
        BigDecimal acumulados = BigDecimal.ZERO;

        for (Repostaje actual : ordenados) {
            // Todo lo repostado suma al tramo en curso, se haya llenado o no.
            acumulados = acumulados.add(actual.getLitros());

            if (!actual.isDepositoLleno()) {
                continue;
            }

            // Es un lleno: cierra el tramo abierto, si es que había uno medible.
            if (llenoAnterior != null && medible(llenoAnterior, actual)) {
                int kilometros = actual.getKilometros() - llenoAnterior.getKilometros();

                tramos.add(new Tramo(
                        kilometros,
                        acumulados,
                        // litros / km * 100, con un decimal: más precisión sería fingir
                        // una exactitud que el método no tiene.
                        acumulados.multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(kilometros), 1, RoundingMode.HALF_UP),
                        actual.getFecha()));
            }

            // Este lleno abre el tramo siguiente; el contador de litros empieza de cero.
            llenoAnterior = actual;
            acumulados = BigDecimal.ZERO;
        }

        return tramos;
    }

    /** Consumo medio de todo el historial, o null si no hay ningún tramo completo. */
    static BigDecimal consumoMedio(List<Repostaje> repostajes) {
        List<Tramo> tramos = tramos(repostajes);
        if (tramos.isEmpty()) {
            return null;
        }

        // Media ponderada por kilómetros, no media de las medias: un tramo de 900 km pesa
        // más que uno de 80. Promediar los consumos sin más daría el mismo valor a los dos
        // y un solo trayecto corto por ciudad torcería el resultado de todo el año.
        BigDecimal litros = tramos.stream()
                .map(Tramo::litros)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int kilometros = tramos.stream().mapToInt(Tramo::kilometros).sum();

        return litros.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(kilometros), 1, RoundingMode.HALF_UP);
    }

    /**
     * Un tramo solo se puede medir si los dos llenados traen cuentakilómetros y este ha
     * avanzado. Si retrocede es que alguien se equivocó al teclear, o que el repostaje es
     * de otro coche: en cualquier caso, el tramo no vale.
     */
    private static boolean medible(Repostaje desde, Repostaje hasta) {
        return desde.getKilometros() != null
                && hasta.getKilometros() != null
                && hasta.getKilometros() > desde.getKilometros();
    }
}
