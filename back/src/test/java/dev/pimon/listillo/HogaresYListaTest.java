package dev.pimon.listillo;

import com.jayway.jsonpath.JsonPath;
import dev.pimon.listillo.hogar.HogarRepository;
import dev.pimon.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hogares y lista de punta a punta: HTTP, seguridad, servicio y Postgres de verdad. Cada
 * test crea sus propias personas, porque la base se comparte entre clases.
 */
class HogaresYListaTest extends ConPostgresEmbebido {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;
    @Autowired
    private HogarRepository hogares;

    private Persona ana;
    private Persona benito;
    private Persona carla;

    @BeforeEach
    void personas() {
        ana = persona("Ana");
        benito = persona("Benito");
        carla = persona("Carla");
    }

    @Test
    @DisplayName("quien crea un hogar lo lleva y recibe un código de 8 caracteres")
    void crearHogar() throws Exception {
        como(ana, post("/api/hogares").content("{\"nombre\":\"  Casa  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Casa"))
                .andExpect(jsonPath("$.data.miRol").value("PROPIETARIO"))
                .andExpect(jsonPath("$.data.codigoInvitacion").value(org.hamcrest.Matchers.matchesPattern("[A-Z2-9]{8}")))
                .andExpect(jsonPath("$.data.miembros", hasSize(1)))
                .andExpect(jsonPath("$.data.miembros[0].nombre").value("Ana"));

        como(ana, get("/api/hogares"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].miembros").value(1));
    }

    @Test
    @DisplayName("el código se acepta en minúsculas y con guion, y entrar dos veces no duplica")
    void unirseConCodigo() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");
        String escrito = casa.codigo().substring(0, 4).toLowerCase() + "-" + casa.codigo().substring(4).toLowerCase();

        como(benito, post("/api/hogares/unirse").content("{\"codigo\":\"" + escrito + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.miRol").value("MIEMBRO"))
                .andExpect(jsonPath("$.data.miembros", hasSize(2)));
        como(benito, post("/api/hogares/unirse").content("{\"codigo\":\"" + casa.codigo() + "\"}"))
                .andExpect(jsonPath("$.data.miembros", hasSize(2)));
    }

    @Test
    @DisplayName("antes de entrar se ve a qué hogar lleva el código, sin miembros ni lista")
    void verInvitacion() throws Exception {
        Hogar casa = crearHogar(ana, "Casa de Ana");

        como(benito, get("/api/hogares/invitacion/" + casa.codigo().toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("Casa de Ana"))
                .andExpect(jsonPath("$.data.miembros").value(1))
                .andExpect(jsonPath("$.data.yaEstoyDentro").value(false))
                .andExpect(jsonPath("$.data.codigoInvitacion").doesNotExist());
        como(ana, get("/api/hogares/invitacion/" + casa.codigo()))
                .andExpect(jsonPath("$.data.yaEstoyDentro").value(true));
        como(benito, get("/api/hogares/invitacion/ZZZZZZZZ")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/hogares/invitacion/" + casa.codigo()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("un código que no existe es un 404 con un mensaje que se entiende")
    void codigoQueNoExiste() throws Exception {
        como(benito, post("/api/hogares/unirse").content("{\"codigo\":\"ZZZZZZZZ\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("quien no es del hogar no ve ni el hogar ni su lista: 404, no 403")
    void losDeFueraNoVenNada() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");

        como(carla, get("/api/hogares/" + casa.id())).andExpect(status().isNotFound());
        como(carla, get("/api/hogares/" + casa.id() + "/lista")).andExpect(status().isNotFound());
        como(carla, post("/api/hogares/" + casa.id() + "/lista").content("{\"nombre\":\"Pan\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sin sesión no se entra a la API de hogares")
    void sinSesion() throws Exception {
        mockMvc.perform(get("/api/hogares"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    @DisplayName("apuntar lo mismo dos veces suma; lo comprado baja al final y se puede vaciar")
    void listaCompartida() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");
        unirse(benito, casa);
        String lista = "/api/hogares/" + casa.id() + "/lista";

        como(ana, post(lista).content("{\"nombre\":\"Leche\"}")).andExpect(status().isOk());
        como(benito, post(lista).content("{\"nombre\":\"Pan\",\"cantidad\":2}")).andExpect(status().isOk());
        como(benito, post(lista).content("{\"nombre\":\"  leche \"}"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].nombre").value("Leche"))
                .andExpect(jsonPath("$.data[0].cantidad").value(2))
                .andExpect(jsonPath("$.data[0].apuntadoPor").value("Ana"));

        String leche = JsonPath.read(como(ana, get(lista)).andReturn().getResponse().getContentAsString(),
                "$.data[0].id");
        como(benito, patch(lista + "/" + leche).content("{\"comprado\":true}"))
                .andExpect(jsonPath("$.data[0].nombre").value("Pan"))
                .andExpect(jsonPath("$.data[1].nombre").value("Leche"))
                .andExpect(jsonPath("$.data[1].comprado").value(true));

        como(ana, delete(lista + "/comprados"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].nombre").value("Pan"));
    }

    @Test
    @DisplayName("un nombre vacío o una cantidad de 100 son un 400")
    void validacion() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");
        String lista = "/api/hogares/" + casa.id() + "/lista";

        como(ana, post(lista).content("{\"nombre\":\"   \"}")).andExpect(status().isBadRequest());
        como(ana, post(lista).content("{\"nombre\":\"Pan\",\"cantidad\":100}")).andExpect(status().isBadRequest());
        como(ana, post("/api/hogares").content("{\"nombre\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("solo el propietario cambia el código, y el viejo deja de valer")
    void nuevoCodigo() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");
        unirse(benito, casa);

        como(benito, post("/api/hogares/" + casa.id() + "/codigo")).andExpect(status().isForbidden());
        como(ana, post("/api/hogares/" + casa.id() + "/codigo")).andExpect(status().isOk());
        como(carla, post("/api/hogares/unirse").content("{\"codigo\":\"" + casa.codigo() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("si se va el propietario lo hereda el más antiguo; si se va el último, el hogar desaparece")
    void salir() throws Exception {
        Hogar casa = crearHogar(ana, "Casa");
        unirse(benito, casa);
        unirse(carla, casa);

        como(ana, delete("/api/hogares/" + casa.id() + "/miembros/yo")).andExpect(status().isNoContent());
        como(benito, get("/api/hogares/" + casa.id()))
                .andExpect(jsonPath("$.data.miRol").value("PROPIETARIO"))
                .andExpect(jsonPath("$.data.miembros", hasSize(2)));

        como(benito, delete("/api/hogares/" + casa.id() + "/miembros/" + carla.id()))
                .andExpect(status().isNoContent());
        como(benito, delete("/api/hogares/" + casa.id() + "/miembros/yo")).andExpect(status().isNoContent());
        assertThat(hogares.existsById(UUID.fromString(casa.id()))).isFalse();
    }

    // ── ayudantes ────────────────────────────────────────────────────────────────

    private record Persona(String id, String token) {
    }

    private record Hogar(String id, String codigo) {
    }

    /** Una cuenta directamente en la tabla de la librería, con su token firmado. */
    private Persona persona(String nombre) {
        String id = UUID.randomUUID().toString();
        String email = nombre.toLowerCase() + "-" + id + "@example.com";
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update("insert into app_users (id, created_at, updated_at, email, name, password, active) "
                + "values (?, ?, ?, ?, ?, 'x', true)", id, ahora, ahora, email, nombre);
        jdbc.update("insert into user_roles (user_id, role) values (?, 'ROLE_USER')", id);
        return new Persona(id, jwt.generateToken(id, email, List.of("ROLE_USER")));
    }

    private Hogar crearHogar(Persona quien, String nombre) throws Exception {
        String cuerpo = como(quien, post("/api/hogares").content("{\"nombre\":\"" + nombre + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Hogar(JsonPath.read(cuerpo, "$.data.id"), JsonPath.read(cuerpo, "$.data.codigoInvitacion"));
    }

    private void unirse(Persona quien, Hogar hogar) throws Exception {
        como(quien, post("/api/hogares/unirse").content("{\"codigo\":\"" + hogar.codigo() + "\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions como(Persona quien, MockHttpServletRequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + quien.token())
                .contentType(MediaType.APPLICATION_JSON));
    }
}
