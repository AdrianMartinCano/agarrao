package dev.pimon.listillo.ticket.lectura;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tickets de ejemplo con la forma de los reales (datos inventados). Cuando haya tickets de
 * verdad, se añaden aquí tal cual salen del PDF o del OCR.
 */
class InterpreteTicketTest {

    private static final String MERCADONA = """
            MERCADONA, S.A. A-46103834
            C/ MAYOR, 12
            28001 MADRID
            TELÉFONO: 910000000
            09/10/2026 19:31  OP: 1234567
            FACTURA SIMPLIFICADA: 2345-021-123456
            Descripción              P. Unit   Importe
            1 LECHE SEMI HACEND 6X1L             5,34
            2 YOGUR GRIEGO           0,95      1,90
            1 PLATANO
            0,734 kg   1,99 €/kg                 1,46
            1 PATATAS FRITAS LISAS               2,30
            TOTAL (€)                           11,00
            TARJETA BANCARIA                    11,00
            IVA      BASE IMPONIBLE (€)   CUOTA (€)
            """;

    private static final String LIDL = """
            LIDL Supermercados S.A.U.
            Avda. de la Constitución 1
            NIF A60195278
            EUR
            PAN DE MOLDE INTEGRAL        1,19 A
            HUEVOS CAMPEROS L            4,38 A
            2 x 2,19
            QUESO RALLADO                1,65 A
            Descuento Lidl Plus         -0,30 A
            TOTAL                        6,92
            05.10.26 11:05
            """;

    /** El PDF de la app de Dia (sin los datos de pago). */
    private static final String DIA = """
            Compra en C/ EJEMPLO 1
            07/10/2026 19:48 Nº de Tienda 10000
            Factura simplificada Nº 1000001-00000001
            Resumen de la compra
            Total sin promociones.....................................8,47 €
            Ofertas y cupones -0,86 €
            Total a pagar................................ 7,61 €
            Forma de pago
            Tarjeta Tef.......................................................7,61 €
            Productos vendidos por Dia
            DESCRIPCIÓN CANTIDAD PRECIO KG TOTAL
            NAPOLITANA
            MIXTA 2 ud 0,75 € 1,50 € A
            NAPOLITANA JAM/AT/BA -0,08 €
            NAPOLITANA
            BARBACOA 2 ud 0,75 € 1,50 € A
            NAPOLITANA JAM/AT/BA -0,08 €
            FLAUTA CHOCO
            ELMOLIN 2 ud 0,39 € 0,78 € A
            FLAUTA CHOCO
            ELMOLIN 2 ud 0,39 € -0,78 € A
            BOLLO CON
            SALCHICHA 2 ud 0,99 € 1,98 € A
            JAMÓN SERRANO
            DIA NT 1 ud 3,49 € 3,49 € A
            JAMÓN SERRANO N.ALAC -0,70 €
            PLATANO 0,734 kg 1,99 € 1,46 € A
            Total venta Dia.................................................9,07 €
            Desglose de IVA
            TIPO IVA BASE CUOTA
            A 10% 6,92 € 0,69 €
            """;

    @Test
    @DisplayName("Dia: resumen arriba, nombre en dos líneas, «2 ud 0,75 € 1,50 €», promociones y devolución")
    void dia() {
        TicketLeido t = InterpreteTicket.leer(DIA);

        assertThat(t.cadena()).isEqualTo(Cadena.DIA);
        assertThat(t.fecha()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(t.total()).isEqualByComparingTo("7.61");
        assertThat(t.lineas()).extracting(LineaLeida::texto).containsExactly(
                "NAPOLITANA MIXTA", "NAPOLITANA JAM/AT/BA", "NAPOLITANA BARBACOA", "NAPOLITANA JAM/AT/BA",
                "BOLLO CON SALCHICHA", "JAMÓN SERRANO DIA NT", "JAMÓN SERRANO N.ALAC", "PLATANO");

        LineaLeida mixta = t.lineas().getFirst();
        assertThat(mixta.cantidad()).isEqualByComparingTo("2");
        assertThat(mixta.precioUnitario()).isEqualByComparingTo("0.75");
        assertThat(mixta.importe()).isEqualByComparingTo("1.50");
        assertThat(t.lineas().get(1).importe()).isEqualByComparingTo("-0.08");

        LineaLeida platano = t.lineas().getLast();
        assertThat(platano.unidad()).isEqualTo(Unidad.KG);
        assertThat(platano.cantidad()).isEqualByComparingTo("0.734");
        assertThat(platano.precioUnitario()).isEqualByComparingTo("1.99");
    }

    @Test
    @DisplayName("Mercadona: cadena, fecha, total, cantidades, precio unitario y artículo a peso")
    void mercadona() {
        TicketLeido t = InterpreteTicket.leer(MERCADONA);

        assertThat(t.cadena()).isEqualTo(Cadena.MERCADONA);
        assertThat(t.fecha()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(t.total()).isEqualByComparingTo("11.00");
        assertThat(t.lineas()).extracting(LineaLeida::texto)
                .containsExactly("LECHE SEMI HACEND 6X1L", "YOGUR GRIEGO", "PLATANO", "PATATAS FRITAS LISAS");

        LineaLeida yogur = t.lineas().get(1);
        assertThat(yogur.cantidad()).isEqualByComparingTo("2");
        assertThat(yogur.precioUnitario()).isEqualByComparingTo("0.95");
        assertThat(yogur.importe()).isEqualByComparingTo("1.90");

        LineaLeida platano = t.lineas().get(2);
        assertThat(platano.unidad()).isEqualTo(Unidad.KG);
        assertThat(platano.cantidad()).isEqualByComparingTo("0.734");
        assertThat(platano.precioUnitario()).isEqualByComparingTo("1.99");
        assertThat(platano.importe()).isEqualByComparingTo("1.46");

        assertThat(t.avisos()).isEmpty();
    }

    @Test
    @DisplayName("Lidl: letra de IVA, «2 x precio» debajo del artículo, descuento y fecha corta")
    void lidl() {
        TicketLeido t = InterpreteTicket.leer(LIDL);

        assertThat(t.cadena()).isEqualTo(Cadena.LIDL);
        assertThat(t.fecha()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(t.lineas()).extracting(LineaLeida::texto)
                .containsExactly("PAN DE MOLDE INTEGRAL", "HUEVOS CAMPEROS L", "QUESO RALLADO", "DESCUENTO LIDL PLUS");
        LineaLeida huevos = t.lineas().get(1);
        assertThat(huevos.cantidad()).isEqualByComparingTo("2");
        assertThat(huevos.precioUnitario()).isEqualByComparingTo("2.19");
        assertThat(huevos.importe()).isEqualByComparingTo("4.38");
        assertThat(t.lineas().get(3).importe()).isEqualByComparingTo("-0.30");
        assertThat(t.avisos()).isEmpty();
    }

    @Test
    @DisplayName("foto con erratas del OCR: O por 0, barras sueltas; si no cuadra el total, avisa")
    void fotoConErratas() {
        TicketLeido t = InterpreteTicket.leer("""
                MERCAD0NA
                | 1 ACEITE OLIVA VIRGEN      5,9O
                1 ATUN CLARO PACK3           3,15
                TOTAL                       12,00
                """);

        assertThat(t.lineas()).extracting(LineaLeida::importe)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("5.90"), new BigDecimal("3.15"));
        assertThat(t.avisos()).singleElement().asString().contains("9,05 €").contains("12,00 €");
    }

    @Test
    @DisplayName("el peso se entiende aunque el OCR destroce la unidad («EUR/KKE»)")
    void pesoConErrata() {
        TicketLeido t = InterpreteTicket.leer("""
                MERCADONA
                1 PLATANO
                0,734 KG 1,99 EUR/KKE 1,46
                TOTAL 1,46
                """);

        assertThat(t.lineas()).singleElement().satisfies(l -> {
            assertThat(l.texto()).isEqualTo("PLATANO");
            assertThat(l.unidad()).isEqualTo(Unidad.KG);
            assertThat(l.importe()).isEqualByComparingTo("1.46");
        });
    }

    @Test
    @DisplayName("texto sin artículos: lo dice en vez de inventar")
    void sinArticulos() {
        TicketLeido t = InterpreteTicket.leer("GRACIAS POR SU VISITA\nHORARIO 9 A 21");

        assertThat(t.lineas()).isEmpty();
        assertThat(t.cadena()).isEqualTo(Cadena.OTRA);
        assertThat(t.avisos()).singleElement().asString().contains("No hemos encontrado artículos");
    }
}
