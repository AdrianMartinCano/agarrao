package dev.pimon.listillo.producto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EanTest {

    @ParameterizedTest
    @ValueSource(strings = {"8480000107947", "96385074", "036000291452", "10012345678902"})
    @DisplayName("acepta EAN-13, EAN-8, UPC-A y GTIN-14 con su dígito de control")
    void validos(String codigo) {
        assertThat(Ean.esValido(codigo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8480000107948", "848000010794", "84800001079470", "abcdefghijklm", "", "8480 00107947"})
    @DisplayName("rechaza dígito de control malo, largos raros y letras")
    void invalidos(String codigo) {
        assertThat(Ean.esValido(codigo)).isFalse();
    }

    @Test
    void nulo() {
        assertThat(Ean.esValido(null)).isFalse();
    }
}
