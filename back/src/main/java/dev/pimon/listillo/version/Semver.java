package dev.pimon.listillo.version;

import java.util.regex.Pattern;

/**
 * Versiones {@code x.y.z}. Solo números: sin sufijos tipo {@code -beta}, que aquí no se
 * usan y complicarían la comparación sin necesidad.
 */
public final class Semver {

    /** El formato que se acepta. Lo usan también las validaciones de los DTO. */
    public static final String PATRON = "^\\d+\\.\\d+\\.\\d+$";

    private static final Pattern REGEX = Pattern.compile(PATRON);

    private Semver() {
    }

    public static boolean esValida(String version) {
        return version != null && REGEX.matcher(version).matches();
    }

    /**
     * Compara número a número, no como texto: como texto, «1.10.0» sería menor que «1.9.0».
     *
     * @return negativo si {@code a < b}, cero si son iguales y positivo si {@code a > b}
     */
    public static int comparar(String a, String b) {
        int[] pa = partes(a);
        int[] pb = partes(b);
        for (int i = 0; i < 3; i++) {
            int diferencia = Integer.compare(pa[i], pb[i]);
            if (diferencia != 0) {
                return diferencia;
            }
        }
        return 0;
    }

    private static int[] partes(String version) {
        if (!esValida(version)) {
            throw new IllegalArgumentException("Versión no válida: " + version);
        }
        String[] trozos = version.split("\\.");
        return new int[] {
                Integer.parseInt(trozos[0]), Integer.parseInt(trozos[1]), Integer.parseInt(trozos[2])
        };
    }
}
