package dev.pimon.listillo.gasto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GastoFijoTest {

    private static GastoFijo netflix(LocalDate primero) {
        return new GastoFijo(UUID.randomUUID(), "Netflix", new BigDecimal("13.99"), TipoGasto.SUSCRIPCION, primero, "yo");
    }

    @Test
    void cadaMesAlLlegarSuDia() {
        GastoFijo fijo = netflix(LocalDate.of(2026, 7, 5));
        assertThat(fijo.pendientes(LocalDate.of(2026, 7, 30))).isEmpty();
        assertThat(fijo.pendientes(LocalDate.of(2026, 10, 4))).extracting(Gasto::getFecha)
                .containsExactly(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 9, 5));
        // Ya apuntados: no se repiten.
        assertThat(fijo.pendientes(LocalDate.of(2026, 10, 4))).isEmpty();
        assertThat(fijo.pendientes(LocalDate.of(2026, 10, 5))).extracting(Gasto::getFecha)
                .containsExactly(LocalDate.of(2026, 10, 5));
    }

    @Test
    void elDiaTreintaYUnoPasaAlVeintiocho() {
        GastoFijo fijo = netflix(LocalDate.of(2026, 1, 31));
        assertThat(fijo.getDia()).isEqualTo((short) 28);
        assertThat(fijo.pendientes(LocalDate.of(2026, 3, 1))).extracting(Gasto::getFecha)
                .containsExactly(LocalDate.of(2026, 2, 28));
    }
}
