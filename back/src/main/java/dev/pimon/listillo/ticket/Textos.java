package dev.pimon.listillo.ticket;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Utilidades de texto para leer y casar tickets. */
public final class Textos {

    private Textos() {
    }

    public static String sinAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * La forma con la que se compara y se aprende: minúsculas, sin acentos, solo letras y
     * números, espacios simples. «LECHE  SEMI HACEND.» y «leche semi hacend» son lo mismo.
     */
    public static String normalizar(String texto) {
        return sinAcentos(texto).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .strip();
    }

    /** Palabras que no dicen qué es el producto («pan DE molde»). «Sin» sí cuenta: sin lactosa. */
    private static final java.util.Set<String> VACIAS =
            java.util.Set.of("de", "del", "la", "las", "el", "los", "y", "en", "al", "con");

    /** Las palabras con letras de al menos dos caracteres, sin las vacías. */
    public static List<String> palabras(String texto) {
        return Arrays.stream(normalizar(texto).split(" "))
                .filter(p -> p.length() >= 2 && p.chars().anyMatch(Character::isLetter) && !VACIAS.contains(p))
                .toList();
    }
}
