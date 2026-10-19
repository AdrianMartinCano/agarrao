package dev.pimon.listillo.ticket;

import dev.pimon.listillo.ticket.dto.SugerenciaDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CasadorTest {

    private static final List<Candidato> LISTA = List.of(
            new Candidato("8480000107947", "Leche semidesnatada Hacendado"),
            new Candidato(null, "Patatas fritas"),
            new Candidato(null, "Pan"));

    @Test
    @DisplayName("las abreviaturas del ticket casan con el nombre entero («SEMI» → semidesnatada)")
    void abreviaturas() {
        SugerenciaDto s = Casador.sugerir("LECHE SEMI HACEND 6X1L", Map.of(), LISTA).orElseThrow();

        assertThat(s.ean()).isEqualTo("8480000107947");
        assertThat(s.origen()).isEqualTo(OrigenSugerencia.PARECIDO);
    }

    @Test
    @DisplayName("un nombre corto de la lista casa aunque el ticket añada marca y formato")
    void nombreCortoDeLaLista() {
        SugerenciaDto s = Casador.sugerir("LECHE SEMI HACEND 6X1L", Map.of(), List.of(new Candidato(null, "Leche semi")))
                .orElseThrow();

        assertThat(s.nombre()).isEqualTo("Leche semi");
    }

    @Test
    @DisplayName("lo aprendido en la cadena gana a cualquier parecido")
    void aprendido() {
        AliasTicket alias = new AliasTicket(java.util.UUID.randomUUID(), Cadena.MERCADONA, "patatas fritas lisas", null, "Patatas fritas");

        SugerenciaDto s = Casador.sugerir("PATATAS FRITAS LISAS", Map.of("patatas fritas lisas", alias), LISTA)
                .orElseThrow();

        assertThat(s.origen()).isEqualTo(OrigenSugerencia.APRENDIDO);
        assertThat(s.nombre()).isEqualTo("Patatas fritas");
    }

    @Test
    @DisplayName("si se parece poco no propone nada: mejor sin casar que mal casado")
    void pocoParecido() {
        assertThat(Casador.sugerir("ATUN CLARO PACK3", Map.of(), LISTA)).isEmpty();
        // «PAN» solo no basta para «PANECILLOS INTEGRALES»: la mitad de las palabras no casan
        assertThat(Casador.sugerir("PANECILLOS INTEGRALES", Map.of(), LISTA)).isEmpty();
    }
}
