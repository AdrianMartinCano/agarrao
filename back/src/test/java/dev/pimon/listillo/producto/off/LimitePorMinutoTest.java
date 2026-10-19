package dev.pimon.listillo.producto.off;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimitePorMinutoTest {

    private Instant ahora = Instant.parse("2026-10-09T10:00:00Z");
    private final Clock reloj = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    };

    @Test
    @DisplayName("no pasa del tope en un minuto y vuelve a dejar cuando el minuto corre")
    void ventanaDeslizante() {
        LimitePorMinuto limite = new LimitePorMinuto(2, reloj);

        assertThat(limite.pedir()).isTrue();
        ahora = ahora.plusSeconds(30);
        assertThat(limite.pedir()).isTrue();
        assertThat(limite.pedir()).isFalse();

        ahora = ahora.plus(Duration.ofSeconds(31)); // el primero ya tiene más de un minuto
        assertThat(limite.pedir()).isTrue();
        assertThat(limite.pedir()).isFalse();
    }
}
