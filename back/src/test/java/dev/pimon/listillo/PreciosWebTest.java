package dev.pimon.listillo;

import com.jayway.jsonpath.JsonPath;
import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;
import dev.pimon.listillo.tienda.CatalogoWeb;
import dev.pimon.listillo.tienda.ImportadorOpenPrices;
import dev.pimon.listillo.tienda.ImportadorPrecios;
import dev.pimon.listillo.tienda.OpenPrices;
import dev.pimon.listillo.tienda.ProductoWeb;
import dev.pimon.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los precios de las webs: importar, adivinar el genérico, enseñarlos en Precios y corregir. */
class PreciosWebTest extends ConPostgresEmbebido {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;
    @Autowired
    private ImportadorPrecios importador;
    @Autowired
    private ImportadorOpenPrices importadorOpenPrices;
    @MockitoBean
    private OpenPrices openPrices;

    /** Una web de mentira con lo que haga falta en cada prueba. */
    private static final class WebFalsa implements CatalogoWeb {
        private final Cadena cadena;
        private final List<ProductoWeb> productos = new ArrayList<>();

        WebFalsa(Cadena cadena) {
            this.cadena = cadena;
        }

        WebFalsa con(String id, String nombre, String precio, String tamano, Unidad unidad, String seccion) {
            BigDecimal euros = new BigDecimal(precio);
            BigDecimal cuanto = new BigDecimal(tamano);
            productos.add(new ProductoWeb(id, nombre, null, euros, cuanto, unidad,
                    euros.divide(cuanto, 3, java.math.RoundingMode.HALF_UP), seccion, null, null));
            return this;
        }

        @Override
        public Cadena cadena() {
            return cadena;
        }

        @Override
        public List<ProductoWeb> leer() {
            return productos;
        }

        @Override
        public Set<Categoria> pasillos(String seccion) {
            return switch (seccion) {
                case "Lácteos" -> Set.of(Categoria.LACTEOS);
                case "Limpieza" -> Set.of(Categoria.LIMPIEZA, Categoria.HOGAR);
                default -> Set.of(Categoria.DESPENSA);
            };
        }
    }

    /** El catálogo web es común a todos los hogares: que no se cuele en los demás tests. */
    @AfterEach
    void limpiar() {
        jdbc.update("delete from productos_tienda");
    }

    @Test
    @DisplayName("lo importado de la web sale en Precios sin tickets, la web manda en su cadena, y se corrige")
    void importarYVer() throws Exception {
        WebFalsa web = new WebFalsa(Cadena.DIA)
                .con("L1", "Leche semidesnatada Dia brik", "0.85", "1", Unidad.L, "Lácteos")
                .con("L2", "Leche semidesnatada sin lactosa Dia", "1.15", "1", Unidad.L, "Lácteos")
                .con("A1", "Arroz redondo Dia", "1.10", "1", Unidad.KG, "Despensa")
                // En litros: no puede ser «Arroz» (que va en kg) aunque se llame así.
                .con("X1", "Bebida de arroz", "1.30", "1", Unidad.L, "Despensa");
        importador.importar(web);

        String yo = token("Ines");
        String hogar = hogar(yo);
        String leche = idDe(yo, hogar, "Leche semidesnatada");
        como(yo, get("/api/hogares/" + hogar + "/genericos"))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Leche semidesnatada')].mejorPrecio").value(0.85))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Leche semidesnatada')].mejorFuente").value("WEB"))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Arroz')].productosWeb").value(1));

        // Con un ticket de Dia más caro, manda la web (es el precio de hoy); Mercadona sale del ticket.
        como(yo, post("/api/hogares/" + hogar + "/tickets").content("""
                {"cadena":"MERCADONA","fecha":"%s","total":6.30,"origen":"PDF","lineas":[
                  {"texto":"LECHE SEMI HACEND 6X1L","cantidad":1,"unidad":"UD","importe":5.34},
                  {"texto":"LECHE SEMIDESNATADA DIA 1L","cantidad":1,"unidad":"UD","importe":0.96}]}"""
                .formatted(java.time.LocalDate.now().minusDays(1)))).andExpect(status().isCreated());
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + leche))
                .andExpect(jsonPath("$.data.tiendas", hasSize(2)))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("DIA"))
                .andExpect(jsonPath("$.data.tiendas[0].fuente").value("WEB"))
                .andExpect(jsonPath("$.data.tiendas[0].nombre").value("Leche semidesnatada Dia brik"))
                .andExpect(jsonPath("$.data.tiendas[1].cadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data.tiendas[1].fuente").value("TICKET"))
                .andExpect(jsonPath("$.data.enLaWeb[0].idExterno").value("L1"));

        // Reclasificar sin leer la web (tras cambiar el catálogo) deja todo igual si nada cambia.
        assertThat(importador.reclasificar(web)).isZero();

        // Corregir uno de la web: deja de contar para la leche y la importación no lo pisa.
        como(yo, put("/api/hogares/" + hogar + "/genericos/clasificacion-web")
                .content("{\"cadena\":\"DIA\",\"idExterno\":\"L1\"}")).andExpect(status().isNoContent());
        importador.importar(web);
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + leche))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("MERCADONA"));

        // Un producto mal clasificado (como antes de una migración) se arregla al reclasificar.
        jdbc.update("update productos_tienda set generico_id = null where id_externo = 'A1'");
        assertThat(importador.reclasificar(web)).isEqualTo(1);
    }

    @Test
    @DisplayName("Open Prices rellena las cadenas sin web, con su fecha; un ticket vuestro más reciente manda")
    void openPrices() throws Exception {
        java.time.LocalDate haceUnMes = java.time.LocalDate.now().minusMonths(1);
        org.mockito.Mockito.when(openPrices.locales()).thenReturn(java.util.Map.of(7L, Cadena.CARREFOUR));
        org.mockito.Mockito.when(openPrices.precios(org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new OpenPrices.PrecioAbierto(Cadena.CARREFOUR, haceUnMes,
                        new ProductoWeb("8431876177401", "Leche semidesnatada Carrefour", "1 l", new BigDecimal("0.80"),
                                BigDecimal.ONE, Unidad.L, new BigDecimal("0.800"), null, null,
                                "https://prices.openfoodfacts.org/products/8431876177401"), null)));
        assertThat(importadorOpenPrices.importar()).containsEntry(Cadena.CARREFOUR, 1);

        String yo = token("Olga");
        String hogar = hogar(yo);
        String leche = idDe(yo, hogar, "Leche semidesnatada");
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + leche))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("CARREFOUR"))
                .andExpect(jsonPath("$.data.tiendas[0].fuente").value("OPEN_PRICES"))
                .andExpect(jsonPath("$.data.tiendas[0].fecha").value(haceUnMes.toString()))
                .andExpect(jsonPath("$.data.enLaWeb[0].fuente").value("OPEN_PRICES"));

        // Un ticket de Carrefour de ayer es más nuevo: manda el ticket.
        como(yo, post("/api/hogares/" + hogar + "/tickets").content("""
                {"cadena":"CARREFOUR","fecha":"%s","total":0.96,"origen":"PDF","lineas":[
                  {"texto":"LECHE SEMIDESNATADA CARREFOUR 1L","cantidad":1,"unidad":"UD","importe":0.96}]}"""
                .formatted(java.time.LocalDate.now().minusDays(1)))).andExpect(status().isCreated());
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + leche))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("CARREFOUR"))
                .andExpect(jsonPath("$.data.tiendas[0].fuente").value("TICKET"));
    }

    @Test
    @DisplayName("un genérico propio recibe lo de las webs por su nombre, y «No es esto» solo lo quita de él")
    void genericoPropioConPreciosDeLasWebs() throws Exception {
        importador.importar(new WebFalsa(Cadena.DIA)
                .con("P1", "Presa ibérica de cerdo", "14.90", "1", Unidad.KG, "Carnes")
                .con("P2", "Presa ibérica extra", "8.00", "0.5", Unidad.KG, "Carnes")
                // Se llama igual pero va en litros: no es lo mismo.
                .con("P3", "Caldo de presa ibérica", "2.00", "1", Unidad.L, "Carnes"));

        String yo = token("Rosa");
        String hogar = hogar(yo);
        String cuerpo = como(yo, post("/api/hogares/" + hogar + "/genericos")
                .content("{\"nombre\":\"Presa ibérica\",\"categoria\":\"CARNE\",\"unidad\":\"KG\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String presa = JsonPath.read(cuerpo, "$.data.id");

        como(yo, get("/api/hogares/" + hogar + "/genericos/" + presa))
                .andExpect(jsonPath("$.data.enLaWeb", hasSize(2)))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("DIA"))
                .andExpect(jsonPath("$.data.tiendas[0].precio").value(14.9));

        como(yo, put("/api/hogares/" + hogar + "/genericos/clasificacion-web")
                .content("{\"cadena\":\"DIA\",\"idExterno\":\"P1\",\"genericoId\":null,\"quitarDe\":\"" + presa + "\"}"))
                .andExpect(status().isNoContent());
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + presa))
                .andExpect(jsonPath("$.data.enLaWeb", hasSize(1)))
                .andExpect(jsonPath("$.data.enLaWeb[0].idExterno").value("P2"));

        // Otra casa con su propia «Presa ibérica» lo sigue viendo: el descarte es de cada genérico.
        String otro = token("Tomas");
        String otraCasa = hogar(otro);
        String suyo = JsonPath.read(como(otro, post("/api/hogares/" + otraCasa + "/genericos")
                .content("{\"nombre\":\"Presa ibérica\",\"categoria\":\"CARNE\",\"unidad\":\"KG\"}"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        como(otro, get("/api/hogares/" + otraCasa + "/genericos/" + suyo))
                .andExpect(jsonPath("$.data.enLaWeb", hasSize(2)));
    }

    @Test
    @DisplayName("un producto del buscador, en cada súper: el mismo nombre y tamaño; si no, otro envase por €/unidad")
    void elMismoProductoEnCadaSuper() throws Exception {
        importador.importar(new WebFalsa(Cadena.DIA)
                .con("N1", "Crema de cacao y avellanas Nutella", "7.29", "0.75", Unidad.KG, "Despensa"));
        importador.importar(new WebFalsa(Cadena.LIDL)
                .con("N2", "Crema al cacao con avellanas Nutella", "7.10", "0.75", Unidad.KG, "Despensa")
                .con("N3", "Crema al cacao con avellanas Nutella", "4.00", "0.35", Unidad.KG, "Despensa")
                // Otra marca con casi el mismo nombre: no es lo mismo.
                .con("N4", "Crema de cacao y avellanas Nocilla", "3.10", "0.75", Unidad.KG, "Despensa"));
        importador.importar(new WebFalsa(Cadena.CARREFOUR)
                .con("N5", "Nutella crema de cacao y avellanas", "4.50", "0.40", Unidad.KG, "Despensa"));

        String yo = token("Lola");
        String hogar = hogar(yo);
        como(yo, get("/api/hogares/" + hogar + "/precios/en-cada-super?cadena=DIA&id=N1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tiendas", hasSize(3)))
                // Por €/kg: Lidl 9,47 (mismo tamaño), Dia 9,72, Carrefour 11,25 (otro tamaño).
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("LIDL"))
                .andExpect(jsonPath("$.data.tiendas[0].producto.idExterno").value("N2"))
                .andExpect(jsonPath("$.data.tiendas[0].mismoTamano").value(true))
                .andExpect(jsonPath("$.data.tiendas[1].cadena").value("DIA"))
                .andExpect(jsonPath("$.data.tiendas[2].cadena").value("CARREFOUR"))
                .andExpect(jsonPath("$.data.tiendas[2].mismoTamano").value(false));
    }

    @Test
    @DisplayName("la lista se reparte por el súper más barato de cada cosa, con lo que costaría ir a una sola")
    void repartirLaLista() throws Exception {
        importador.importar(new WebFalsa(Cadena.DIA)
                .con("L1", "Leche semidesnatada Dia brik", "0.85", "1", Unidad.L, "Lácteos")
                .con("A1", "Arroz redondo Dia", "1.40", "1", Unidad.KG, "Despensa"));
        importador.importar(new WebFalsa(Cadena.MERCADONA)
                .con("M1", "Leche semidesnatada Hacendado", "0.95", "1", Unidad.L, "Lácteos")
                .con("M2", "Arroz redondo Hacendado", "1.15", "1", Unidad.KG, "Despensa"));

        String yo = token("Julia");
        String hogar = hogar(yo);
        String lista = "/api/hogares/" + hogar + "/lista";
        como(yo, post(lista).content("{\"nombre\":\"Leche semidesnatada\",\"cantidad\":6}"));
        como(yo, post(lista).content("{\"nombre\":\"Arroz\"}"));
        como(yo, post(lista).content("{\"nombre\":\"Pilas AAA\"}"));

        como(yo, get(lista + "/reparto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grupos", hasSize(2)))
                .andExpect(jsonPath("$.data.grupos[?(@.cadena == 'DIA')].articulos[0].nombre").value("Leche semidesnatada"))
                .andExpect(jsonPath("$.data.grupos[?(@.cadena == 'DIA')].total").value(5.10))
                .andExpect(jsonPath("$.data.grupos[?(@.cadena == 'MERCADONA')].articulos[0].producto").value("Arroz redondo Hacendado"))
                .andExpect(jsonPath("$.data.total").value(6.25))
                .andExpect(jsonPath("$.data.sinPrecio[0].nombre").value("Pilas AAA"))
                // Solo en Mercadona: 6 × 0,95 + 1,15 = 6,85; solo en Dia: 6 × 0,85 + 1,40 = 6,50.
                .andExpect(jsonPath("$.data.enUna[0].cadena").value("DIA"))
                .andExpect(jsonPath("$.data.enUna[0].total").value(6.5))
                .andExpect(jsonPath("$.data.enUna[1].total").value(6.85));
        como(token("Intrusa"), get(lista + "/reparto")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("se busca cualquier cosa en todas las webs (también el bazar) y sale lo más barato de cada una")
    void buscarEnTodasLasTiendas() throws Exception {
        importador.importar(new WebFalsa(Cadena.MERCADONA)
                .con("F1", "Fregona microfibra Bosque Verde", "2.50", "1", Unidad.UD, "Limpieza")
                .con("F2", "Fregona tiras algodón Bosque Verde", "1.95", "1", Unidad.UD, "Limpieza")
                .con("C1", "Cubo con escurridor", "4.00", "1", Unidad.UD, "Limpieza"));
        importador.importar(new WebFalsa(Cadena.CONSUM)
                .con("X1", "Fregona Algodón Consum", "1.79", "1", Unidad.UD, "Limpieza"));

        String yo = token("Kika");
        String hogar = hogar(yo);
        como(yo, get("/api/hogares/" + hogar + "/precios/buscar").param("q", "fregona"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.porUnidad").value(true))
                .andExpect(jsonPath("$.data.tiendas", hasSize(2)))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("CONSUM"))
                .andExpect(jsonPath("$.data.tiendas[0].productos[0].precio").value(1.79))
                .andExpect(jsonPath("$.data.tiendas[1].cadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data.tiendas[1].encontrados").value(2))
                .andExpect(jsonPath("$.data.tiendas[1].productos[0].nombre").value("Fregona tiras algodón Bosque Verde"));
        // Sin acentos ni mayúsculas, y varias palabras.
        como(yo, get("/api/hogares/" + hogar + "/precios/buscar").param("q", "FREGONA algodon"))
                .andExpect(jsonPath("$.data.tiendas", hasSize(2)));
        // En plural también: «fregonas» encuentra «Fregona».
        como(yo, get("/api/hogares/" + hogar + "/precios/buscar").param("q", "fregonas"))
                .andExpect(jsonPath("$.data.tiendas", hasSize(2)));
        // Ajustes: con solo Mercadona elegido, Consum desaparece de la búsqueda.
        como(yo, put("/api/hogares/" + hogar + "/cadenas").content("{\"cadenas\":[\"MERCADONA\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cadenas", hasSize(1)));
        como(yo, get("/api/hogares/" + hogar + "/precios/buscar").param("q", "fregona"))
                .andExpect(jsonPath("$.data.tiendas", hasSize(1)))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("MERCADONA"));
        como(yo, put("/api/hogares/" + hogar + "/cadenas").content("{\"cadenas\":[]}"))
                .andExpect(status().isBadRequest());
        como(yo, get("/api/hogares/" + hogar + "/precios/buscar").param("q", "fr"))
                .andExpect(status().isBadRequest());
        como(token("Extraña"), get("/api/hogares/" + hogar + "/precios/buscar").param("q", "fregona"))
                .andExpect(status().isNotFound());
    }

    private String idDe(String token, String hogar, String nombre) throws Exception {
        String cuerpo = como(token, get("/api/hogares/" + hogar + "/genericos")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(cuerpo, "$.data[?(@.nombre == '" + nombre + "')].id");
        return ids.getFirst();
    }

    private String hogar(String token) throws Exception {
        String cuerpo = como(token, post("/api/hogares").content("{\"nombre\":\"Casa\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.data.id");
    }

    private String token(String nombre) {
        String id = UUID.randomUUID().toString();
        String email = nombre.toLowerCase() + "-" + id + "@example.com";
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update("insert into app_users (id, created_at, updated_at, email, name, password, active) "
                + "values (?, ?, ?, ?, ?, 'x', true)", id, ahora, ahora, email, nombre);
        jdbc.update("insert into user_roles (user_id, role) values (?, 'ROLE_USER')", id);
        return jwt.generateToken(id, email, List.of("ROLE_USER"));
    }

    private ResultActions como(String token, MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }
}
