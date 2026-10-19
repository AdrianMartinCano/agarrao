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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Gastos que no son del súper: anotarlos, los fijos de cada mes y las partes. */
class GastosAnotadosTest extends ConPostgresEmbebido {

    private static final LocalDate HOY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;

    @Test
    @DisplayName("lo anotado suma al gasto del mes junto a los tickets, repartido por tipo")
    void anotarYVerloEnElGasto() throws Exception {
        String yo = token("Marta");
        String hogar = hogar(yo);
        como(yo, post("/api/hogares/" + hogar + "/tickets").content("""
                {"cadena":"MERCADONA","fecha":"%s","total":20.00,"origen":"PDF",
                 "lineas":[{"texto":"LECHE","cantidad":1,"unidad":"UD","importe":20.00}]}""".formatted(HOY)))
                .andExpect(status().isCreated());
        String cena = anotar(yo, hogar, "Cena La Tagliatella", "46.80", HOY, "RESTAURANTE", false);
        anotar(yo, hogar, "Gasolina", "60.00", HOY, "GASOLINA", false);

        como(yo, get("/api/hogares/" + hogar + "/gasto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(126.8))
                .andExpect(jsonPath("$.data.tickets").value(1))
                .andExpect(jsonPath("$.data.anotados").value(2))
                .andExpect(jsonPath("$.data.porTipo[0].tipo").value("GASOLINA"))
                .andExpect(jsonPath("$.data.porTipo[1].tipo").value("RESTAURANTE"))
                .andExpect(jsonPath("$.data.porTipo[2].tipo").value("SUPER"))
                .andExpect(jsonPath("$.data.porTipo[2].total").value(20.0))
                .andExpect(jsonPath("$.data.meses[5].total").value(126.8));

        como(yo, put("/api/hogares/" + hogar + "/gastos/" + cena).content("""
                {"concepto":"Cena con los primos","importe":50.00,"fecha":"%s","tipo":"RESTAURANTE"}""".formatted(HOY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.concepto").value("Cena con los primos"));
        como(yo, get("/api/hogares/" + hogar + "/gastos"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.tipo == 'RESTAURANTE')].importe").value(50.0));

        como(yo, delete("/api/hogares/" + hogar + "/gastos/" + cena)).andExpect(status().isNoContent());
        como(yo, get("/api/hogares/" + hogar + "/gasto")).andExpect(jsonPath("$.data.total").value(80.0));
    }

    @Test
    @DisplayName("un gasto fijo se apunta solo cada mes; quitarlo deja lo ya apuntado")
    void gastoFijoDeCadaMes() throws Exception {
        String yo = token("Pablo");
        String hogar = hogar(yo);
        // Desde hace dos meses: se apuntan ese, el siguiente y el de este mes si ya llegó su día.
        LocalDate primero = HOY.minusMonths(2).withDayOfMonth(Math.min(HOY.getDayOfMonth(), 28));
        anotar(yo, hogar, "Netflix", "13.99", primero, "SUSCRIPCION", true);

        String fijos = como(yo, get("/api/hogares/" + hogar + "/gastos/fijos"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].dia").value(primero.getDayOfMonth()))
                .andReturn().getResponse().getContentAsString();
        String fijo = JsonPath.read(fijos, "$.data[0].id");

        for (int atras = 0; atras <= 2; atras++) {
            YearMonth mes = YearMonth.from(primero).plusMonths(atras);
            como(yo, get("/api/hogares/" + hogar + "/gastos?mes=" + mes))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].concepto").value("Netflix"));
        }
        // Mirar otra vez no lo apunta dos veces.
        como(yo, get("/api/hogares/" + hogar + "/gasto"));
        como(yo, get("/api/hogares/" + hogar + "/gastos")).andExpect(jsonPath("$.data", hasSize(1)));

        como(yo, delete("/api/hogares/" + hogar + "/gastos/fijos/" + fijo)).andExpect(status().isNoContent());
        como(yo, get("/api/hogares/" + hogar + "/gastos/fijos")).andExpect(jsonPath("$.data", hasSize(0)));
        como(yo, get("/api/hogares/" + hogar + "/gastos?mes=" + YearMonth.from(primero)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("los datos se validan y los gastos de un hogar son solo suyos")
    void validacionYAcceso() throws Exception {
        String yo = token("Sara");
        String hogar = hogar(yo);
        como(yo, post("/api/hogares/" + hogar + "/gastos").content("""
                {"concepto":"","importe":-3,"fecha":"%s","tipo":"OCIO"}""".formatted(HOY.plusDays(2))))
                .andExpect(status().isBadRequest());
        String cine = anotar(yo, hogar, "Cine", "18.00", HOY, "OCIO", false);

        String otro = token("Intruso");
        como(otro, get("/api/hogares/" + hogar + "/gastos")).andExpect(status().isNotFound());
        como(otro, delete("/api/hogares/" + hogar(otro) + "/gastos/" + cine)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cada persona elige qué partes usa; sin elegir, todas")
    void partes() throws Exception {
        String yo = token("Leo");
        como(yo, get("/api/yo/partes"))
                .andExpect(jsonPath("$.data", containsInAnyOrder("LISTA", "PRECIOS", "GASTOS", "GASOLINA")));
        como(yo, put("/api/yo/partes").content("{\"partes\":[\"GASTOS\",\"LISTA\"]}"))
                .andExpect(status().isOk());
        como(yo, get("/api/yo/partes")).andExpect(jsonPath("$.data", containsInAnyOrder("LISTA", "GASTOS")));
        como(yo, put("/api/yo/partes").content("{\"partes\":[]}")).andExpect(status().isBadRequest());
        // Las de otro no cambian.
        como(token("Ana"), get("/api/yo/partes")).andExpect(jsonPath("$.data", hasSize(4)));
    }

    private String anotar(String token, String hogar, String concepto, String importe, LocalDate fecha, String tipo,
                          boolean cadaMes) throws Exception {
        String cuerpo = como(token, post("/api/hogares/" + hogar + "/gastos").content("""
                {"concepto":"%s","importe":%s,"fecha":"%s","tipo":"%s","cadaMes":%s}"""
                .formatted(concepto, importe, fecha, tipo, cadaMes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.data.id");
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
