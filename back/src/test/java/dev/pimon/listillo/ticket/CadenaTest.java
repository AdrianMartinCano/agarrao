package dev.pimon.listillo.ticket;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CadenaTest {

    @Test
    void reconoceLaCadenaEnLaCabecera() {
        assertThat(Cadena.detectar(List.of("FAMILY CASH", "C/ Mayor 1"))).isEqualTo(Cadena.FAMILY_CASH);
        assertThat(Cadena.detectar(List.of("Familycash S.L.", "NIF B12345678"))).isEqualTo(Cadena.FAMILY_CASH);
        assertThat(Cadena.detectar(List.of("COSTCO WHOLESALE SPAIN", "Getafe"))).isEqualTo(Cadena.COSTCO);
        assertThat(Cadena.detectar(List.of("Mercadona, S.A."))).isEqualTo(Cadena.MERCADONA);
        assertThat(Cadena.detectar(List.of("Fruteria Paqui"))).isEqualTo(Cadena.OTRA);
    }
}
