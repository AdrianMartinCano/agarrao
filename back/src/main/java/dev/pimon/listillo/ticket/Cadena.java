package dev.pimon.listillo.ticket;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Las cadenas que se reconocen en la cabecera del ticket. Sirve para el aprendizaje: «LECHE
 * SEMI HACEND» quiere decir una cosa en Mercadona y en otra cadena puede no existir.
 */
public enum Cadena {
    MERCADONA("MERCADONA"),
    LIDL("LIDL"),
    CARREFOUR("CARREFOUR"),
    DIA("\\bDIA\\b", "DIA RETAIL", "DISTRIBUIDORA INTERNACIONAL DE ALIMENTACION"),
    ALCAMPO("ALCAMPO", "AUCHAN"),
    EROSKI("EROSKI"),
    ALDI("\\bALDI\\b"),
    CONSUM("CONSUM"),
    AHORRAMAS("AHORRAMAS"),
    EL_CORTE_INGLES("HIPERCOR", "SUPERCOR", "EL CORTE INGLES"),
    GADIS("GADIS"),
    BONPREU("BONPREU", "BONPREU I ESCLAT"),
    CAPRABO("CAPRABO"),
    FAMILY_CASH("FAMILY ?CASH"),
    COSTCO("COSTCO"),
    OTRA();

    private final List<Pattern> patrones;

    Cadena(String... patrones) {
        this.patrones = java.util.Arrays.stream(patrones).map(Pattern::compile).toList();
    }

    /** Mira las primeras líneas (la cabecera): ahí va el nombre, no en los artículos. */
    public static Cadena detectar(List<String> lineas) {
        List<String> cabecera = lineas.subList(0, Math.min(12, lineas.size()));
        for (Cadena cadena : values()) {
            for (String linea : cabecera) {
                String mayusculas = Textos.sinAcentos(linea).toUpperCase();
                if (cadena.patrones.stream().anyMatch(p -> p.matcher(mayusculas).find())) {
                    return cadena;
                }
            }
        }
        return OTRA;
    }
}
