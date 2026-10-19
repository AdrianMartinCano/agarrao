package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Unidad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdivinadorTest {

    private static final List<Generico> CATALOGO = List.of(
            generico("Leche entera", "en:whole-milks,leche entera"),
            generico("Leche semidesnatada", "en:semi-skimmed-milks,leche semi"),
            generico("Huevos", "en:eggs,huevo"),
            generico("Patatas", "patata"),
            generico("Patatas fritas de bolsa", "en:crisps,patatas fritas,chips"),
            generico("Tomates", "tomate pera"),
            generico("Tomate frito", "en:tomato-sauces,tomate frito"),
            generico("Sal", "sal marina"),
            generico("Salmón", "salmon"),
            generico("Plátanos", "platano,banana"),
            generico("Jamón cocido", "en:cooked-hams,york"),
            generico("Galletas", "en:biscuits,galleta"),
            generico("Chocolate", "en:chocolates,chocolate negro,chocolate leche"));

    @ParameterizedTest(name = "«{0}» es {1}")
    @CsvSource(delimiter = ';', value = {
            "LECHE SEMI HACEND 6X1L; Leche semidesnatada",
            "LECHE ENTERA 1L; Leche entera",
            "PATATAS FRITAS LISAS; Patatas fritas de bolsa",
            "PATATA MALLA 3KG; Patatas",
            "TOMATE FRITO HACENDADO; Tomate frito",
            "SAL MARINA; Sal",
            "SALMON FILETE; Salmón",
            "PLATANO CANARIAS; Plátanos",
            "HUEVOS L 12 UDS; Huevos",
            "JAMON YORK EXTRA; Jamón cocido",
    })
    void porNombre(String texto, String esperado) {
        assertThat(adivinar(null, texto)).map(Generico::getNombre).contains(esperado);
    }

    @Test
    void loQueVaDelanteEsLoQueEs() {
        assertThat(adivinar(null, "Galletas Digestive chocolate con leche Hacendado"))
                .map(Generico::getNombre).contains("Galletas");
        assertThat(adivinar(null, "Chocolate con leche Hacendado")).map(Generico::getNombre).contains("Chocolate");
    }

    @Test
    void enLaWebLaPrimeraPalabraManda() {
        assertThat(Adivinador.adivinarPorCabeza("Bollo con pepitas de chocolate Hacendado", CATALOGO)).isEmpty();
        assertThat(Adivinador.adivinarPorCabeza("Leche semidesnatada Hacendado", CATALOGO))
                .map(Generico::getNombre).contains("Leche semidesnatada");
    }

    @Test
    void patatasCongeladasYHamburguesasVanASuSitio() {
        // Las pistas como quedan tras V15.
        List<Generico> catalogo = List.of(
                generico("Patatas", "patata,malla patata,-lisas,-onduladas,-fritas,-chips,-gajo,-bravas,-prefritas,-congelad,-horno"),
                generico("Patatas fritas de bolsa", "patatas fritas,patatas lisas,-prefritas,-congelad,-horno,-gajo"),
                generico("Patatas congeladas", "patatas prefritas,patatas congeladas,patatas horno,patatas gajo"),
                generico("Carne picada", "picada,carne picada,-hamburguesa,-burger,-ajo,-perejil"),
                generico("Hamburguesas", "hamburguesa,burger meat,burger"));
        assertThat(Adivinador.adivinarPorCabeza("Patatas Prefritas Corte Grueso Consum", catalogo))
                .map(Generico::getNombre).contains("Patatas congeladas");
        assertThat(Adivinador.adivinarPorCabeza("Patatas Horno Extra Crujiente Consum", catalogo))
                .map(Generico::getNombre).contains("Patatas congeladas");
        assertThat(Adivinador.adivinarPorCabeza("Patatas fritas clásicas Hacendado", catalogo))
                .map(Generico::getNombre).contains("Patatas fritas de bolsa");
        assertThat(Adivinador.adivinarPorCabeza("Patata malla 5kg", catalogo)).map(Generico::getNombre).contains("Patatas");
        assertThat(Adivinador.adivinarPorCabeza("Burger meat mixta cerdo y vacuno ElPozo", catalogo))
                .map(Generico::getNombre).contains("Hamburguesas");
        assertThat(Adivinador.adivinarPorCabeza("Picada de ajo y perejil", catalogo)).isEmpty();
        assertThat(Adivinador.adivinarPorCabeza("Carne picada de vacuno", catalogo))
                .map(Generico::getNombre).contains("Carne picada");
        // Mercadona, Consum y Ahorramás la llaman «Preparado de…»: lo de delante no cuenta.
        assertThat(Adivinador.adivinarPorCabeza("Preparado de carne picada vacuno y cerdo", catalogo))
                .map(Generico::getNombre).contains("Carne picada");
        assertThat(Adivinador.adivinarPorCabeza("Preparado Picada Mixta Consum", catalogo))
                .map(Generico::getNombre).contains("Carne picada");
    }

    @Test
    void unaPistaConMenosExcluye() {
        List<Generico> soloHuevos = List.of(generico("Huevos", "huevo,-codorniz"));
        assertThat(Adivinador.adivinar(null, List.of("Huevos de codorniz"), soloHuevos)).isEmpty();
        assertThat(Adivinador.adivinar(null, List.of("Huevos grandes L"), soloHuevos)).isPresent();
    }

    @Test
    void sinParecidoNoPropone() {
        assertThat(adivinar(null, "ATUN CLARO PACK3")).isEmpty();
        assertThat(adivinar(null, "BOLSA")).isEmpty();
    }

    @Test
    void laCategoriaDeOpenFoodFactsMasConcretaGana() {
        assertThat(adivinar("en:dairies,en:milks,en:semi-skimmed-milks", "Leche entera Hacendado"))
                .map(Generico::getNombre).contains("Leche semidesnatada");
    }

    @Test
    void elPrimerNombreFiableGanaAIgualdad() {
        assertThat(Adivinador.adivinar(null, Arrays.asList(null, "Huevos", "LECHE ENTERA"), CATALOGO))
                .map(Generico::getNombre).contains("Huevos");
    }

    private static Optional<Generico> adivinar(String categorias, String texto) {
        return Adivinador.adivinar(categorias, List.of(texto), CATALOGO);
    }

    private static Generico generico(String nombre, String pistas) {
        Generico g = new Generico(null, nombre, Categoria.OTROS, Unidad.KG);
        ReflectionTestUtils.setField(g, "pistas", pistas);
        return g;
    }
}
