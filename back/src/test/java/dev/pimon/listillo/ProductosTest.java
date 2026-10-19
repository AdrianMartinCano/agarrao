package dev.pimon.listillo;

import com.jayway.jsonpath.JsonPath;
import dev.pimon.listillo.producto.CatalogoProductos;
import dev.pimon.listillo.producto.DatosProducto;
import dev.pimon.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Productos por código de barras de punta a punta, con un catálogo falso en vez de Open
 * Food Facts (los tests no salen a internet). Solo conoce la leche Hacendado.
 */
@Import(ProductosTest.CatalogoFalso.class)
class ProductosTest extends ConPostgresEmbebido {

    private static final String LECHE = "8480000107947";
    private static final String DESCONOCIDO = "96385074";

    @TestConfiguration
    static class CatalogoFalso {
        @Bean
        @Primary
        CatalogoProductos catalogoFalso() {
            return ean -> LECHE.equals(ean)
                    ? Optional.of(new DatosProducto("Leche semidesnatada", "Hacendado", "1 L", null, "en:milks"))
                    : Optional.empty();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;

    @Test
    @DisplayName("un código conocido devuelve el producto citando a Open Food Facts")
    void conocido() throws Exception {
        String yo = token("Ana");
        como(yo, get("/api/productos/" + LECHE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Leche semidesnatada"))
                .andExpect(jsonPath("$.data.marca").value("Hacendado"))
                .andExpect(jsonPath("$.data.fuente").value("Open Food Facts"));
    }

    @Test
    @DisplayName("uno desconocido es 404 hasta que alguien lo nombra; uno mal leído es 400")
    void desconocidoYNombrado() throws Exception {
        String yo = token("Benito");
        como(yo, get("/api/productos/" + DESCONOCIDO)).andExpect(status().isNotFound());
        como(yo, get("/api/productos/12345678")).andExpect(status().isBadRequest());

        como(yo, put("/api/productos/" + DESCONOCIDO).content("{\"nombre\":\"Turrón de la abuela\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origen").value("USUARIO"))
                .andExpect(jsonPath("$.data.fuente").doesNotExist());
        como(token("Carla"), get("/api/productos/" + DESCONOCIDO))
                .andExpect(jsonPath("$.data.nombre").value("Turrón de la abuela"));
    }

    @Test
    @DisplayName("sin sesión no se puede usar de proxy hacia Open Food Facts")
    void sinSesion() throws Exception {
        mockMvc.perform(get("/api/productos/" + LECHE))
                .andExpect(r -> assertThat(r.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("escanear el mismo envase dos veces suma, aunque el nombre se haya cambiado")
    void escanearDosVecesSuma() throws Exception {
        String yo = token("Dani");
        String cuerpo = como(yo, post("/api/hogares").content("{\"nombre\":\"Casa\"}"))
                .andReturn().getResponse().getContentAsString();
        String lista = "/api/hogares/" + JsonPath.read(cuerpo, "$.data.id") + "/lista";

        como(yo, post(lista).content("{\"nombre\":\"Leche semidesnatada Hacendado\",\"ean\":\"" + LECHE + "\"}"))
                .andExpect(jsonPath("$.data[0].ean").value(LECHE));
        como(yo, post(lista).content("{\"nombre\":\"Leche\",\"ean\":\"" + LECHE + "\"}"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].cantidad").value(2));
        como(yo, post(lista).content("{\"nombre\":\"Algo\",\"ean\":\"8480000107948\"}"))
                .andExpect(status().isBadRequest());
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
        return mockMvc.perform(peticion
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }
}
