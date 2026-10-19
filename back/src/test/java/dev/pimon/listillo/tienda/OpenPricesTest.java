package dev.pimon.listillo.tienda;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Lectura de locales y precios de Open Prices, con su forma real (recortada). */
class OpenPricesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void laCadenaSaleDeLaMarcaODelNombreYSoloEnEspana() throws Exception {
        assertThat(OpenPrices.cadenaDe(JSON.readTree("""
                {"id": 1, "osm_brand": "Carrefour Express", "osm_name": "Express", "osm_address_country_code": "ES"}""")))
                .contains(Cadena.CARREFOUR);
        assertThat(OpenPrices.cadenaDe(JSON.readTree("""
                {"id": 2, "osm_brand": null, "osm_name": "Dia&Go", "osm_address_country_code": "ES"}""")))
                .contains(Cadena.DIA);
        assertThat(OpenPrices.cadenaDe(JSON.readTree("""
                {"id": 3, "osm_brand": "Mi Alcampo", "osm_address_country_code": "ES"}"""))).contains(Cadena.ALCAMPO);
        assertThat(OpenPrices.cadenaDe(JSON.readTree("""
                {"id": 4, "osm_brand": "Fruteria Paqui", "osm_address_country_code": "ES"}"""))).isEmpty();
        assertThat(OpenPrices.cadenaDe(JSON.readTree("""
                {"id": 5, "osm_brand": "Carrefour", "osm_address_country_code": "FR"}"""))).isEmpty();
    }

    @Test
    void preciosDeProductosEnEurosConSuTamano() throws Exception {
        List<OpenPrices.PrecioAbierto> leidos = OpenPrices.leerPrecios(JSON.readTree("""
                {"items": [
                  {"id": 1, "location_id": 10, "type": "PRODUCT", "product_code": "8480012017913", "price": 2.49,
                   "currency": "EUR", "date": "2025-10-27",
                   "product": {"code": "8480012017913", "product_name": "Ensalada cesar.", "brands": "Eliges, IFA RETAIL S.A",
                     "product_quantity": 205, "product_quantity_unit": "g",
                     "image_url": "https://images.openfoodfacts.org/x.jpg", "categories_tags": ["en:meals", "en:prepared-salads"]}},
                  {"id": 2, "location_id": 10, "type": "CATEGORY", "category_tag": "en:apples", "price": 1.99,
                   "currency": "EUR", "date": "2025-10-27", "product": null},
                  {"id": 3, "location_id": 99, "type": "PRODUCT", "product_code": "1", "price": 1, "currency": "EUR",
                   "date": "2025-10-27", "product": {"product_name": "De otra tienda"}},
                  {"id": 4, "location_id": 10, "type": "PRODUCT", "product_code": "2", "price": 1.5, "currency": "EUR",
                   "date": "2025-10-28", "product": {"product_name": "Agua mineral", "product_quantity": 150,
                     "product_quantity_unit": "cl"}}
                ]}"""), Map.of(10L, Cadena.CARREFOUR));

        assertThat(leidos).hasSize(2);
        OpenPrices.PrecioAbierto ensalada = leidos.getFirst();
        assertThat(ensalada.cadena()).isEqualTo(Cadena.CARREFOUR);
        assertThat(ensalada.fecha()).isEqualTo(LocalDate.of(2025, 10, 27));
        assertThat(ensalada.categoriasOff()).isEqualTo("en:meals,en:prepared-salads");
        ProductoWeb w = ensalada.producto();
        assertThat(w.idExterno()).isEqualTo("8480012017913");
        assertThat(w.nombre()).isEqualTo("Ensalada cesar Eliges");
        assertThat(w.precio()).isEqualByComparingTo("2.49");
        assertThat(w.tamano()).isEqualByComparingTo("0.205");
        assertThat(w.unidad()).isEqualTo(Unidad.KG);
        assertThat(w.precioUnidad()).isEqualByComparingTo("12.146");
        assertThat(w.formato()).isEqualTo("205 g");
        assertThat(w.url()).isEqualTo("https://prices.openfoodfacts.org/products/8480012017913");
        assertThat(leidos.get(1).producto().tamano()).isEqualByComparingTo("1.5");
        assertThat(leidos.get(1).producto().unidad()).isEqualTo(Unidad.L);
    }

    @Test
    void sinCantidadEsUnaPieza() {
        assertThat(OpenPrices.medida(0, "").unidad()).isEqualTo(Unidad.UD);
        assertThat(OpenPrices.medida(6, "uds").tamano()).isEqualByComparingTo("1");
        assertThat(OpenPrices.medida(1, "kg").tamano()).isEqualByComparingTo("1");
    }

    @Test
    void deCadaProductoSeQuedaElPrecioMasReciente() {
        List<OpenPrices.PrecioAbierto> ultimos = ImportadorOpenPrices.ultimos(List.of(
                precio(Cadena.DIA, "2025-05-01", "1.00"),
                precio(Cadena.DIA, "2026-01-10", "1.20"),
                precio(Cadena.CARREFOUR, "2025-01-01", "0.90")));
        assertThat(ultimos).hasSize(2);
        assertThat(ultimos).filteredOn(p -> p.cadena() == Cadena.DIA).singleElement()
                .extracting(p -> p.producto().precio()).isEqualTo(new BigDecimal("1.20"));
    }

    private static OpenPrices.PrecioAbierto precio(Cadena cadena, String fecha, String euros) {
        BigDecimal precio = new BigDecimal(euros);
        return new OpenPrices.PrecioAbierto(cadena, LocalDate.parse(fecha),
                new ProductoWeb("848", "Leche", null, precio, BigDecimal.ONE, Unidad.L, precio, null, null, null), null);
    }
}
