package dev.pimon.listillo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La aplicación entera contra un Postgres real, con Flyway y {@code ddl-auto: validate}:
 * lo mismo que pasa en el servidor. Si una entidad —propia o de una librería— no cuadra
 * con las migraciones, el contexto no arranca y este test falla con el nombre de lo que
 * falta.
 *
 * Aquí van también las pruebas del endpoint de versión.
 */
class ContextoArrancaTest extends ConPostgresEmbebido {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("la versión se consulta sin sesión: tiene que poder bloquear también el login")
    void versionEsPublica() throws Exception {
        mockMvc.perform(get("/api/app/version").param("actual", "1.2.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("AL_DIA"))
                .andExpect(jsonPath("$.data.ultima").value("1.2.4"));
        // Con la de antes, la nueva se ofrece (V28) pero no se obliga.
        mockMvc.perform(get("/api/app/version").param("actual", "0.1.0"))
                .andExpect(jsonPath("$.data.estado").value("OPCIONAL"));
    }

    @Test
    @DisplayName("una versión por debajo de la mínima es obligatoria")
    void porDebajoDeLaMinimaEsObligatoria() throws Exception {
        mockMvc.perform(get("/api/app/version").param("actual", "0.0.9"))
                .andExpect(jsonPath("$.data.estado").value("OBLIGATORIA"));
    }

    @Test
    @DisplayName("una versión mal escrita es un 400, no un 500")
    void entradaMalaEs400() throws Exception {
        mockMvc.perform(get("/api/app/version").param("actual", "uno"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cambiar la versión publicada pide sesión")
    void elPanelNoEsPublico() throws Exception {
        mockMvc.perform(put("/api/admin/app/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ultima\":\"9.9.9\",\"minima\":\"9.9.9\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int codigo = result.getResponse().getStatus();
                    if (codigo != 401 && codigo != 403) {
                        throw new AssertionError("Esperaba 401 o 403 y llegó " + codigo);
                    }
                });
    }

    @Test
    @DisplayName("no se pueden crear cuentas con contraseña: se entra solo con Google")
    void registroCerrado() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@example.com\",\"password\":\"Listillo1234!\"}"))
                .andExpect(status().isNotFound());
    }
}
