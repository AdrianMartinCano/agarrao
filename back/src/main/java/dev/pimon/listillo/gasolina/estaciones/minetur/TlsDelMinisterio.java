package dev.pimon.listillo.gasolina.estaciones.minetur;

import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Lo que necesita la JVM para hablar con el servidor de precios del Ministerio. Viene de
 * GasPathApplication (Gasofa): tiene que llamarse en el {@code main}, ANTES de
 * arrancar nada, porque Java lee estas propiedades una sola vez al iniciar TLS.
 */
public final class TlsDelMinisterio {

    private TlsDelMinisterio() {
    }

    public static void preparar() {
        permitirCifradoAntiguo();
        completarCadenasIncompletas();
    }

    /**
     * Si un servidor manda su certificado sin el intermedio, que Java lo busque en la URL
     * que el propio certificado dice (AIA «CA Issuers»). Sin esto falla con «PKIX path
     * building failed». No cambia en quién se confía.
     */
    private static void completarCadenasIncompletas() {
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
    }

    /**
     * Vuelve a permitir los cifrados TLS_RSA_*: el servidor del Ministerio solo ofrece
     * TLS_RSA_WITH_AES_256_GCM_SHA384 y desde Java 21.0.12 están en
     * {@code jdk.tls.disabledAlgorithms} (el síntoma es un «Connection reset» en el
     * handshake, sin mencionar los cifrados). Lo que viaja son precios públicos: no hay
     * nada que proteger de una escucha. Rehabilitar no es usar: cualquier servidor que
     * ofrezca algo mejor sigue negociando lo más fuerte. El resto de prohibidos se queda.
     */
    private static void permitirCifradoAntiguo() {
        String prohibidos = Security.getProperty("jdk.tls.disabledAlgorithms");
        if (prohibidos == null || !prohibidos.contains("TLS_RSA_")) {
            return;
        }
        Security.setProperty("jdk.tls.disabledAlgorithms", Arrays.stream(prohibidos.split(","))
                .map(String::trim)
                .filter(algoritmo -> !algoritmo.equals("TLS_RSA_*"))
                .collect(Collectors.joining(", ")));
    }
}
