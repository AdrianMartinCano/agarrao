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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La gasolina traída de Gasofa: coches del hogar y repostajes en Gasto. */
class GasolinaTest extends ConPostgresEmbebido {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;

    @Test
    @DisplayName("el coche es de la casa: lo ve quien entra después y su repostaje cuenta una vez como gasolina")
    void cocheDelHogarYRepostajeEnElGasto() throws Exception {
        String ana = token("Ana");
        String cuerpo = como(ana, post("/api/hogares").content("{\"nombre\":\"Casa\"}"))
                .andReturn().getResponse().getContentAsString();
        String hogar = JsonPath.read(cuerpo, "$.data.id");
        String codigo = JsonPath.read(cuerpo, "$.data.codigoInvitacion");

        String coche = JsonPath.read(como(ana, post("/api/hogares/" + hogar + "/vehiculos").content("""
                {"nombre":"El Dacia","carburante":"GASOLINA_95_E5"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        como(ana, post("/api/hogares/" + hogar + "/repostajes").content("""
                {"vehiculoId":"%s","fecha":"%s","carburante":"GASOLINA_95_E5","litros":40,"precioLitro":1.5}"""
                .formatted(coche, LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importe").value(60.0));

        String benito = token("Benito");
        como(benito, post("/api/hogares/unirse").content("{\"codigo\":\"" + codigo + "\"}")).andExpect(status().isOk());
        como(benito, get("/api/hogares/" + hogar + "/vehiculos"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nombre").value("El Dacia"));
        como(benito, get("/api/hogares/" + hogar + "/repostajes")).andExpect(jsonPath("$.data", hasSize(1)));

        como(benito, get("/api/hogares/" + hogar + "/gasto"))
                .andExpect(jsonPath("$.data.total").value(60.0))
                .andExpect(jsonPath("$.data.repostajes").value(1))
                .andExpect(jsonPath("$.data.porTipo[0].tipo").value("GASOLINA"))
                .andExpect(jsonPath("$.data.porTipo[0].total").value(60.0));

        como(token("Intruso"), get("/api/hogares/" + hogar + "/vehiculos")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("buscar gasolineras no pide sesión; los repostajes sí")
    void buscadorPublico() throws Exception {
        mockMvc.perform(get("/api/estaciones/estado")).andExpect(status().isOk());
        mockMvc.perform(get("/api/estaciones/carburantes")).andExpect(status().isOk());
        mockMvc.perform(get("/api/hogares/" + UUID.randomUUID() + "/repostajes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la gasolina es una parte más y sale encendida también para quien ya había elegido")
    void parteGasolina() throws Exception {
        String leo = token("Leo");
        como(leo, get("/api/yo/partes")).andExpect(jsonPath("$.data", hasSize(4)));
        // Alguien que eligió antes de que existiera Gasolina (fila de V22 pasada por V25).
        String marta = token("Marta-gasolina");
        String martaId = jdbc.queryForObject("select id from app_users where name = 'Marta-gasolina'", String.class);
        jdbc.update("insert into partes_usuario (usuario_id, apagadas) values (?, 'PRECIOS')", martaId);
        como(marta, get("/api/yo/partes"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[2]").value("GASOLINA"));
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
