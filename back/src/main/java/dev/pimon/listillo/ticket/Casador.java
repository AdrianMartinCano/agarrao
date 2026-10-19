package dev.pimon.listillo.ticket;

import dev.pimon.listillo.ticket.dto.SugerenciaDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Propone con qué producto casar una línea de ticket.
 *
 * Primero lo aprendido (alias de esa cadena). Si no, el candidato del hogar (su lista y
 * tickets anteriores) que mejor encaja:
 * <ul>
 *   <li>Lo que más cuenta es cuánto del nombre de la lista aparece en el ticket: «Leche
 *       semi» está entero en «LECHE SEMI HACEND 6X1L», aunque el ticket lleve además la
 *       marca y el formato.</li>
 *   <li>El ticket abrevia y la lista no: «SEMI» casa con «semidesnatada» y «HACEND» con
 *       «Hacendado» (la palabra de la lista empieza por la del ticket). Al revés no:
 *       «PANECILLOS» no es «Pan».</li>
 * </ul>
 */
final class Casador {

    /** Por debajo de esto no se propone nada: mejor una línea sin casar que una mal casada. */
    static final double UMBRAL = 0.6;
    /** Peso de «cuánto del nombre de la lista está en el ticket» frente a lo contrario. */
    private static final double PESO_CANDIDATO = 0.7;
    private static final int ABREVIATURA_MINIMA = 3;

    private Casador() {
    }

    static Optional<SugerenciaDto> sugerir(String textoLinea, Map<String, AliasTicket> aprendidos,
                                           Collection<Candidato> candidatos) {
        AliasTicket alias = aprendidos.get(Textos.normalizar(textoLinea));
        if (alias != null) {
            return Optional.of(new SugerenciaDto(alias.getEan(), alias.getNombre(), OrigenSugerencia.APRENDIDO));
        }

        List<String> delTicket = Textos.palabras(textoLinea);
        if (delTicket.isEmpty()) {
            return Optional.empty();
        }
        Candidato mejor = null;
        double mejorNota = 0;
        for (Candidato c : candidatos) {
            double nota = parecido(delTicket, Textos.palabras(c.nombre()));
            if (nota > mejorNota) {
                mejorNota = nota;
                mejor = c;
            }
        }
        return mejorNota >= UMBRAL
                ? Optional.of(new SugerenciaDto(mejor.ean(), mejor.nombre(), OrigenSugerencia.PARECIDO))
                : Optional.empty();
    }

    static double parecido(List<String> delTicket, List<String> delCandidato) {
        if (delCandidato.isEmpty() || delTicket.isEmpty()) {
            return 0;
        }
        long candidatoEnTicket = delCandidato.stream()
                .filter(c -> delTicket.stream().anyMatch(t -> casan(t, c)))
                .count();
        if (candidatoEnTicket == 0) {
            return 0;
        }
        long ticketEnCandidato = delTicket.stream()
                .filter(t -> delCandidato.stream().anyMatch(c -> casan(t, c)))
                .count();
        return PESO_CANDIDATO * candidatoEnTicket / delCandidato.size()
                + (1 - PESO_CANDIDATO) * ticketEnCandidato / delTicket.size();
    }

    /** Igual, o la palabra del ticket es una abreviatura (prefijo) de la de la lista. */
    private static boolean casan(String delTicket, String delCandidato) {
        return delTicket.equals(delCandidato)
                || (delTicket.length() >= ABREVIATURA_MINIMA && delCandidato.startsWith(delTicket));
    }
}
