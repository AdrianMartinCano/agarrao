package dev.pimon.listillo.producto.off;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ventana deslizante de un minuto: como mucho {@code tope} permisos en los últimos 60 s.
 * Basta para un solo proceso (Listillo corre en un contenedor); con varios habría que
 * llevarlo a la base o a Redis.
 */
final class LimitePorMinuto {

    private final int tope;
    private final Clock reloj;
    private final Deque<Instant> usados = new ArrayDeque<>();

    LimitePorMinuto(int tope, Clock reloj) {
        this.tope = tope;
        this.reloj = reloj;
    }

    synchronized boolean pedir() {
        Instant ahora = reloj.instant();
        Instant haceUnMinuto = ahora.minusSeconds(60);
        while (!usados.isEmpty() && !usados.peekFirst().isAfter(haceUnMinuto)) {
            usados.pollFirst();
        }
        if (usados.size() >= tope) {
            return false;
        }
        usados.addLast(ahora);
        return true;
    }
}
