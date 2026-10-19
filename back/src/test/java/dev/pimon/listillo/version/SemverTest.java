package dev.pimon.listillo.version;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemverTest {

    @Test
    @DisplayName("compara número a número, no como texto")
    void comparaComoNumeros() {
        // Como texto, "1.10.0" < "1.9.0". Es el fallo clásico.
        assertThat(Semver.comparar("1.10.0", "1.9.0")).isPositive();
        assertThat(Semver.comparar("1.2.3", "1.2.3")).isZero();
        assertThat(Semver.comparar("0.9.9", "1.0.0")).isNegative();
    }

    @Test
    @DisplayName("solo acepta x.y.z")
    void soloAceptaTresNumeros() {
        assertThat(Semver.esValida("1.0.0")).isTrue();
        assertThat(Semver.esValida("1.0")).isFalse();
        assertThat(Semver.esValida("1.0.0-beta")).isFalse();
        assertThat(Semver.esValida(null)).isFalse();
        assertThatThrownBy(() -> Semver.comparar("1.0", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
