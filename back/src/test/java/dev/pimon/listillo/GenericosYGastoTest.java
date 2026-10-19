package dev.pimon.listillo;

import com.jayway.jsonpath.JsonPath;
import dev.pimon.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Genéricos de punta a punta: clasificar solos, comparar por €/unidad, corregir y el gasto. */
class GenericosYGastoTest extends ConPostgresEmbebido {

    /** Fechas relativas a hoy: los precios de más de 180 días dejan de contar. */
    private static final LocalDate DIA = LocalDate.now().minusDays(1);
    private static final LocalDate MES_ANTERIOR = DIA.minusMonths(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;

    @Test
    @DisplayName("los productos de los tickets se clasifican solos y se comparan por €/unidad entre tiendas")
    void compararPorUnidad() throws Exception {
        String yo = token("Elena");
        String hogar = hogar(yo);
        String mercadona = ticket(yo, hogar, "MERCADONA", DIA, 9.83, """
                {"texto":"LECHE SEMI HACEND 6X1L","cantidad":1,"unidad":"UD","importe":5.34},
                {"texto":"PLATANO CANARIAS","cantidad":0.85,"unidad":"KG","precioUnitario":1.99,"importe":1.69},
                {"texto":"PATATAS FRITAS LISAS","cantidad":1,"unidad":"UD","importe":2.30},
                {"texto":"ZZZ COSA RARA","cantidad":1,"unidad":"UD","importe":1.00},
                {"texto":"DESCUENTO","cantidad":1,"unidad":"UD","importe":-0.50}""");
        ticket(yo, hogar, "LIDL", DIA, 0.95, """
                {"texto":"LECHE SEMIDESNATADA 1L","cantidad":1,"unidad":"UD","importe":0.95}""");

        String leche = idDe(yo, hogar, "Leche semidesnatada");
        como(yo, get("/api/hogares/" + hogar + "/genericos"))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Leche semidesnatada')].mejorPrecio").value(0.89))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Leche semidesnatada')].mejorCadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Leche semidesnatada')].productos").value(2))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Plátanos')].mejorPrecio").value(1.988))
                .andExpect(jsonPath("$.data[?(@.nombre == 'Huevos')].mejorPrecio", hasItem(nullValue())));

        como(yo, get("/api/hogares/" + hogar + "/genericos/" + leche))
                .andExpect(jsonPath("$.data.unidad").value("L"))
                .andExpect(jsonPath("$.data.tiendas", hasSize(2)))
                .andExpect(jsonPath("$.data.tiendas[0].cadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data.tiendas[0].precio").value(0.89))
                .andExpect(jsonPath("$.data.tiendas[1].cadena").value("LIDL"))
                .andExpect(jsonPath("$.data.productos[0].tamano").value(6))
                .andExpect(jsonPath("$.data.productos[0].origen").value("AUTO"));

        // Desde el ticket: la leche con su tamaño y su precio; la cosa rara, sin genérico.
        como(yo, get("/api/hogares/" + hogar + "/genericos/ticket/" + mercadona))
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].generico").value("Leche semidesnatada"))
                .andExpect(jsonPath("$.data[0].precioUnidad").value(0.89))
                .andExpect(jsonPath("$.data[3].genericoId").value(nullValue()));
    }

    @Test
    @DisplayName("se corrige el tamaño o el genérico a mano, y un genérico propio recoge lo pendiente")
    void corregirYCrear() throws Exception {
        String yo = token("Fermin");
        String hogar = hogar(yo);
        ticket(yo, hogar, "MERCADONA", DIA, 5.30, """
                {"texto":"PATATAS FRITAS LISAS","cantidad":2,"unidad":"UD","importe":2.30},
                {"texto":"TURRON BLANDO 300G","cantidad":1,"unidad":"UD","importe":3.00}""");
        String patatas = idDe(yo, hogar, "Patatas fritas de bolsa");

        // Sin tamaño no hay precio; con «cada bolsa trae 0,150 kg», 2,30 / (2 × 0,15).
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + patatas))
                .andExpect(jsonPath("$.data.productos[0].precioUnidad").value(nullValue()));
        como(yo, put("/api/hogares/" + hogar + "/genericos/clasificacion").content("""
                {"cadena":"MERCADONA","texto":"Patatas fritas lisas","genericoId":"%s","tamano":0.15}""".formatted(patatas)))
                .andExpect(status().isNoContent());
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + patatas))
                .andExpect(jsonPath("$.data.productos[0].precioUnidad").value(closeTo(7.667, 0.001)))
                .andExpect(jsonPath("$.data.productos[0].origen").value("USUARIO"));

        // El turrón no está en el catálogo; al crearlo, el del ticket pasa a ser suyo.
        String cuerpo = como(yo, post("/api/hogares/" + hogar + "/genericos")
                .content("{\"nombre\":\"Turrón\",\"categoria\":\"DESAYUNO\",\"unidad\":\"KG\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.propio").value(true))
                .andReturn().getResponse().getContentAsString();
        String turron = JsonPath.read(cuerpo, "$.data.id");
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + turron))
                .andExpect(jsonPath("$.data.tiendas[0].precio").value(10.0));
        como(yo, post("/api/hogares/" + hogar + "/genericos")
                .content("{\"nombre\":\"turrón\",\"categoria\":\"DESAYUNO\",\"unidad\":\"KG\"}"))
                .andExpect(status().isConflict());

        // «No es ningún genérico»: deja de contar y no se vuelve a adivinar.
        como(yo, put("/api/hogares/" + hogar + "/genericos/clasificacion").content("""
                {"cadena":"MERCADONA","texto":"PATATAS FRITAS LISAS"}"""))
                .andExpect(status().isNoContent());
        como(yo, get("/api/hogares/" + hogar + "/genericos/" + patatas))
                .andExpect(jsonPath("$.data.productos", hasSize(0)));

        // Lo propio es de cada hogar; el catálogo no se borra.
        String otro = token("Gema");
        String suyo = hogar(otro);
        como(otro, get("/api/hogares/" + suyo + "/genericos/" + turron)).andExpect(status().isNotFound());
        como(yo, delete("/api/hogares/" + hogar + "/genericos/" + patatas)).andExpect(status().isForbidden());
        como(yo, delete("/api/hogares/" + hogar + "/genericos/" + turron)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("el gasto sale por mes, tienda, categoría y genérico; los descuentos van aparte")
    void gasto() throws Exception {
        String yo = token("Hugo");
        String hogar = hogar(yo);
        ticket(yo, hogar, "MERCADONA", DIA, 8.33, """
                {"texto":"LECHE SEMI HACEND 6X1L","cantidad":1,"unidad":"UD","importe":5.34},
                {"texto":"PLATANO CANARIAS","cantidad":0.85,"unidad":"KG","importe":1.69},
                {"texto":"ZZZ COSA RARA","cantidad":1,"unidad":"UD","importe":1.80},
                {"texto":"DESCUENTO","cantidad":1,"unidad":"UD","importe":-0.50}""");
        ticket(yo, hogar, "LIDL", DIA, 0.95, """
                {"texto":"LECHE SEMIDESNATADA 1L","cantidad":1,"unidad":"UD","importe":0.95}""");
        ticket(yo, hogar, "LIDL", MES_ANTERIOR, 12.00, """
                {"texto":"ACEITE GIRASOL 1L","cantidad":6,"unidad":"UD","importe":12.00}""");

        como(yo, get("/api/hogares/" + hogar + "/gasto?mes=" + YearMonth.from(DIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(9.28))
                .andExpect(jsonPath("$.data.tickets").value(2))
                .andExpect(jsonPath("$.data.descuentos").value(0.5))
                .andExpect(jsonPath("$.data.referencia").value(12.0))
                .andExpect(jsonPath("$.data.meses", hasSize(6)))
                .andExpect(jsonPath("$.data.meses[?(@.mes == '" + YearMonth.from(DIA) + "')].total").value(9.28))
                .andExpect(jsonPath("$.data.meses[?(@.mes == '" + YearMonth.from(MES_ANTERIOR) + "')].total").value(12.0))
                .andExpect(jsonPath("$.data.porTienda[0].cadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data.porTienda[1].total").value(0.95))
                .andExpect(jsonPath("$.data.porCategoria[0].categoria").value("LACTEOS"))
                .andExpect(jsonPath("$.data.porCategoria[0].total").value(6.29))
                .andExpect(jsonPath("$.data.porCategoria[?(@.categoria == null)].total").value(1.8))
                .andExpect(jsonPath("$.data.top[0].nombre").value("Leche semidesnatada"))
                .andExpect(jsonPath("$.data.top[0].compras").value(2));

        // Elegir el mes anterior no esconde el de hoy: los tarritos siguen acabando en él.
        como(yo, get("/api/hogares/" + hogar + "/gasto?mes=" + YearMonth.from(MES_ANTERIOR)))
                .andExpect(jsonPath("$.data.total").value(12.0))
                .andExpect(jsonPath("$.data.meses", hasSize(6)))
                .andExpect(jsonPath("$.data.meses[5].mes").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.data.meses[?(@.mes == '" + YearMonth.from(MES_ANTERIOR) + "')].total").value(12.0));
        // Uno de hace un año se ve al final de los suyos.
        como(yo, get("/api/hogares/" + hogar + "/gasto?mes=" + YearMonth.now().minusMonths(12)))
                .andExpect(jsonPath("$.data.meses[5].mes").value(YearMonth.now().minusMonths(12).toString()));

        como(yo, get("/api/hogares/" + hogar + "/gasto?mes=octubre")).andExpect(status().isBadRequest());
        como(token("Intruso"), get("/api/hogares/" + hogar + "/gasto")).andExpect(status().isNotFound());
    }

    private String ticket(String token, String hogar, String cadena, LocalDate fecha, double total, String lineas)
            throws Exception {
        String cuerpo = como(token, post("/api/hogares/" + hogar + "/tickets").content("""
                {"cadena":"%s","fecha":"%s","total":%s,"origen":"PDF","lineas":[%s]}"""
                .formatted(cadena, fecha, total, lineas)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.data.ticket.id");
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
