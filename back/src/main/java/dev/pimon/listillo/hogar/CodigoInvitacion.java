package dev.pimon.listillo.hogar;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Códigos de invitación de 8 caracteres sin los que se confunden al dictarlos por teléfono
 * (0/O, 1/I/L). 31^8 ≈ 850.000 millones: probar códigos al azar no sale a cuenta.
 */
final class CodigoInvitacion {

    static final int LONGITUD = 8;
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    private CodigoInvitacion() {
    }

    static String nuevo() {
        StringBuilder codigo = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            codigo.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    /** Lo que escribe la gente: minúsculas, espacios o guiones («abcd-efgh»). */
    static String normalizar(String escrito) {
        return escrito.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
