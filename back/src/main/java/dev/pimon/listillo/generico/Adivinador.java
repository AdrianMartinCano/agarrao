package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Textos;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adivina el genérico de un producto de ticket.
 * <ol>
 *   <li>Por la categoría de Open Food Facts, si el producto tiene código de barras: la más
 *       concreta que sea de un genérico (OFF las da de general a concreta).</li>
 *   <li>Por las palabras del nombre (el del producto, el de la lista o el del ticket) frente al
 *       nombre del genérico y sus otras formas de llamarlo. Como al casar tickets, el ticket
 *       abrevia: «SEMI» vale por «semidesnatada»; pero una palabra igual pesa más que una
 *       abreviatura, para que «SAL MARINA» sea Sal y no Salmón.</li>
 * </ol>
 * Mejor sin genérico que con uno equivocado: por debajo del umbral no se propone nada.
 */
public final class Adivinador {

    static final double UMBRAL = 0.6;
    private static final double PESO_GENERICO = 0.7;
    private static final double ABREVIATURA = 0.85;
    private static final int ABREVIATURA_MINIMA = 3;
    /**
     * Lo que se suma, solo para ordenar, cuando el genérico casa con la PRIMERA palabra del
     * producto: en español lo que es la cosa va delante («Galletas Digestive con chocolate»
     * son galletas, aunque la pista «chocolate leche» case con más palabras).
     */
    private static final double CABEZA = 0.1;

    private Adivinador() {
    }

    /**
     * @param categoriasOff las de Open Food Facts del producto («en:dairies,en:milks,…»), o null
     * @param nombres       lo que se sabe de cómo se llama, de más a menos fiable
     */
    public static Optional<Generico> adivinar(String categoriasOff, List<String> nombres, Collection<Generico> genericos) {
        return porCategoriaOff(categoriasOff, genericos).or(() -> porNombre(nombres, genericos, false));
    }

    /**
     * Para nombres completos (el catálogo web de una cadena): el genérico tiene que casar con
     * la PRIMERA palabra, que es lo que es la cosa. «Bollo con pepitas de chocolate» no es
     * Chocolate; «Cuartos traseros de pollo» se queda sin genérico antes que mal casado.
     */
    public static Optional<Generico> adivinarPorCabeza(String nombre, Collection<Generico> genericos) {
        return porNombre(List.of(nombre), genericos, true);
    }

    private static Optional<Generico> porCategoriaOff(String categoriasOff, Collection<Generico> genericos) {
        if (categoriasOff == null || categoriasOff.isBlank()) {
            return Optional.empty();
        }
        List<String> etiquetas = Arrays.stream(categoriasOff.split(",")).map(String::strip).toList();
        Generico mejor = null;
        int mejorPosicion = -1;
        for (Generico g : genericos) {
            for (String c : g.categoriasOff()) {
                int posicion = etiquetas.indexOf(c);
                if (posicion > mejorPosicion) {
                    mejorPosicion = posicion;
                    mejor = g;
                }
            }
        }
        return Optional.ofNullable(mejor);
    }

    private static Optional<Generico> porNombre(List<String> nombres, Collection<Generico> genericos,
                                               boolean cabezaObligatoria) {
        Generico mejor = null;
        double mejorNota = 0;
        int mejorPosicion = Integer.MAX_VALUE;
        for (String nombre : nombres) {
            if (nombre == null) {
                continue;
            }
            List<String> delProducto = sinEnvoltorio(sinCifras(Textos.palabras(nombre)));
            if (delProducto.isEmpty()) {
                continue;
            }
            List<String> todas = Textos.palabras(nombre);
            for (Generico g : genericos) {
                if (excluido(todas, g.excluidas())) {
                    continue;
                }
                List<String> delGenerico = Textos.palabras(g.getNombre());
                double nota = parecido(delProducto, delGenerico);
                for (String otro : g.otrosNombres()) {
                    double notaOtro = parecido(delProducto, Textos.palabras(otro));
                    if (notaOtro > nota) {
                        nota = notaOtro;
                        delGenerico = Textos.palabras(otro);
                    }
                }
                // A igualdad gana lo que va antes en el nombre: en español lo que es la cosa
                // va delante («Galletas de chocolate» son galletas). Y si no, el primer nombre,
                // que es el más fiable.
                int posicion = primeraCoincidencia(delProducto, delGenerico);
                if (nota < UMBRAL || (cabezaObligatoria && posicion != 0)) {
                    continue;
                }
                double orden = nota + (posicion == 0 ? CABEZA : 0);
                if (orden > mejorNota + EMPATE || (Math.abs(orden - mejorNota) <= EMPATE && posicion < mejorPosicion)) {
                    mejorNota = orden;
                    mejor = g;
                    mejorPosicion = posicion;
                }
            }
        }
        return mejorNota >= UMBRAL ? Optional.of(mejor) : Optional.empty();
    }

    private static final double EMPATE = 1e-9;

    /** Alguna palabra del producto empieza por una de las excluidas («codorniz», «carcasa»). */
    private static boolean excluido(List<String> delProducto, List<String> excluidas) {
        return !excluidas.isEmpty() && delProducto.stream().anyMatch(p -> excluidas.stream().anyMatch(p::startsWith));
    }

    private static int primeraCoincidencia(List<String> delProducto, List<String> delGenerico) {
        for (int i = 0; i < delProducto.size(); i++) {
            String p = delProducto.get(i);
            if (delGenerico.stream().anyMatch(g -> casan(p, g) > 0)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Palabras que van delante pero no dicen qué es la cosa: «Preparado de carne picada
     * vacuno» es carne picada (así la llaman Mercadona, Consum y Ahorramás).
     */
    private static final java.util.Set<String> ENVOLTORIOS = java.util.Set.of("preparado", "preparados");

    private static List<String> sinEnvoltorio(List<String> palabras) {
        int i = 0;
        while (i < palabras.size() - 1 && ENVOLTORIOS.contains(palabras.get(i))) {
            i++;
        }
        return palabras.subList(i, palabras.size());
    }

    /** «6x1l» o «500g» no dicen qué es: fuera antes de comparar. */
    private static List<String> sinCifras(List<String> palabras) {
        return palabras.stream().filter(p -> p.chars().noneMatch(Character::isDigit)).toList();
    }

    static double parecido(List<String> delProducto, List<String> delGenerico) {
        if (delGenerico.isEmpty() || delProducto.isEmpty()) {
            return 0;
        }
        double genericoEnProducto = 0;
        for (String g : delGenerico) {
            genericoEnProducto += delProducto.stream().mapToDouble(p -> casan(p, g)).max().orElse(0);
        }
        if (genericoEnProducto == 0) {
            return 0;
        }
        double productoEnGenerico = 0;
        for (String p : delProducto) {
            productoEnGenerico += delGenerico.stream().mapToDouble(g -> casan(p, g)).max().orElse(0);
        }
        return PESO_GENERICO * genericoEnProducto / delGenerico.size()
                + (1 - PESO_GENERICO) * productoEnGenerico / delProducto.size();
    }

    /** 1 si es la misma palabra; menos si la del producto abrevia la del genérico. */
    private static double casan(String delProducto, String delGenerico) {
        if (delProducto.equals(delGenerico)) {
            return 1;
        }
        return delProducto.length() >= ABREVIATURA_MINIMA && delGenerico.startsWith(delProducto) ? ABREVIATURA : 0;
    }
}
