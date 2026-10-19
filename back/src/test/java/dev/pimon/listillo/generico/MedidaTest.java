package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Unidad;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MedidaTest {

    @ParameterizedTest(name = "«{0}» = {1} {2}")
    @CsvSource(delimiter = ';', value = {
            "LECHE SEMI HACEND 6X1L; 6; L",
            "6 x 1 l; 6; L",
            "1 L; 1; L",
            "1,5 L; 1.5; L",
            "1'5 litros; 1.5; L",
            "750ML; 0.75; L",
            "33 cl; 0.33; L",
            "500 g; 0.5; KG",
            "YOGUR NAT 4X125G; 0.5; KG",
            "ARROZ REDONDO 1KG; 1; KG",
            "HUEVOS L 12 UDS; 12; UD",
            "papel higienico 6 rollos; 6; UD",
            "HUEVOS CAMPEROS DOCENA; 12; UD",
            "MEDIA DOCENA HUEVOS; 6; UD",
    })
    void lee(String texto, BigDecimal valor, Unidad unidad) {
        Medida m = Medida.leer(texto).orElseThrow();
        assertThat(m.valor()).isEqualByComparingTo(valor);
        assertThat(m.unidad()).isEqualTo(unidad);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"PATATAS FRITAS LISAS", "PLATANO CANARIAS", "ATUN CLARO PACK3"})
    void sinMedida(String texto) {
        assertThat(Medida.leer(texto)).isEmpty();
    }

    @org.junit.jupiter.api.Test
    void kilosNoSonLitros() {
        assertThat(Medida.leer("500 g").orElseThrow().en(Unidad.L)).isEmpty();
        assertThat(Medida.leer("500 g").orElseThrow().en(Unidad.KG)).contains(new BigDecimal("0.5000"));
    }
}
