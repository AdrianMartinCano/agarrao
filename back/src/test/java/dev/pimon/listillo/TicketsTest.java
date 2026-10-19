package dev.pimon.listillo;

import com.jayway.jsonpath.JsonPath;
import dev.pimon.security.jwt.JwtTokenProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tickets de punta a punta: leer (PDF y texto), repasar, guardar, aprender y tachar. */
class TicketsTest extends ConPostgresEmbebido {

    private static final String[] TICKET = {
            "MERCADONA, S.A. A-46103834",
            "05/10/2026 19:31 OP: 1234567",
            "Descripcion P. Unit Importe",
            "1 LECHE SEMI HACEND 6X1L 5,34",
            "1 PATATAS FRITAS LISAS 2,30",
            "TOTAL (EUR) 7,64",
    };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtTokenProvider jwt;

    @Test
    @DisplayName("un PDF de Mercadona sale leído y casado con lo que hay en la lista")
    void leerPdf() throws Exception {
        String yo = token("Ana");
        String hogar = hogar(yo);
        como(yo, post("/api/hogares/" + hogar + "/lista").content("{\"nombre\":\"Leche semidesnatada Hacendado\"}"));

        como(yo, multipart("/api/hogares/" + hogar + "/tickets/leer-pdf")
                .file(new MockMultipartFile("archivo", "ticket.pdf", "application/pdf", pdf(
                        "MERCADONA, S.A. A-46103834", "05/10/2026 19:31 OP: 1234567", "Descripcion P. Unit Importe",
                        "1 LECHE SEMI HACEND 6X1L 5,34", "1 ATUN CLARO PACK3 2,30", "TOTAL (EUR) 7,64"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cadena").value("MERCADONA"))
                .andExpect(jsonPath("$.data.fecha").value("2026-10-05"))
                .andExpect(jsonPath("$.data.total").value(7.64))
                .andExpect(jsonPath("$.data.lineas", hasSize(2)))
                .andExpect(jsonPath("$.data.lineas[0].sugerencia.nombre").value("Leche semidesnatada Hacendado"))
                .andExpect(jsonPath("$.data.lineas[0].sugerencia.origen").value("PARECIDO"))
                .andExpect(jsonPath("$.data.lineas[1].sugerencia").doesNotExist());
    }

    @Test
    @DisplayName("algo que no es un PDF es un 400; quien no es del hogar, 404")
    void errores() throws Exception {
        String yo = token("Benito");
        String hogar = hogar(yo);
        como(yo, multipart("/api/hogares/" + hogar + "/tickets/leer-pdf")
                .file(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5, 6})))
                .andExpect(status().isBadRequest());
        como(token("Intruso"), post("/api/hogares/" + hogar + "/tickets/leer-texto").content("{\"texto\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("al guardar, lo casado se aprende para la cadena y se tacha de la lista")
    void guardarAprendeYTacha() throws Exception {
        String yo = token("Carla");
        String hogar = hogar(yo);
        String lista = "/api/hogares/" + hogar + "/lista";
        como(yo, post(lista).content("{\"nombre\":\"Patatas fritas\"}"));

        como(yo, post("/api/hogares/" + hogar + "/tickets").content("""
                {"cadena":"MERCADONA","fecha":"2026-10-05","total":7.64,"origen":"FOTO","lineas":[
                  {"texto":"LECHE SEMI HACEND 6X1L","cantidad":1,"unidad":"UD","importe":5.34},
                  {"texto":"PATATAS FRITAS LISAS","cantidad":1,"unidad":"UD","importe":2.30,"nombre":"Patatas fritas"}
                ]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tachados").value(1))
                .andExpect(jsonPath("$.data.ticket.lineas", hasSize(2)));
        como(yo, get(lista)).andExpect(jsonPath("$.data[0].comprado").value(true));

        // El siguiente ticket de Mercadona del mismo hogar ya sale casado por lo aprendido…
        String ticket = "{\"texto\":\"" + String.join("\\n", TICKET) + "\"}";
        como(yo, post("/api/hogares/" + hogar + "/tickets/leer-texto").content(ticket))
                .andExpect(jsonPath("$.data.lineas[1].sugerencia.nombre").value("Patatas fritas"))
                .andExpect(jsonPath("$.data.lineas[1].sugerencia.origen").value("APRENDIDO"));
        // …pero en otro hogar no: lo aprendido es de cada casa.
        String otra = token("Dani");
        como(otra, post("/api/hogares/" + hogar(otra) + "/tickets/leer-texto").content(ticket))
                .andExpect(jsonPath("$.data.lineas[1].sugerencia").doesNotExist());

        como(yo, get("/api/hogares/" + hogar + "/tickets"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lineas").value(2))
                .andExpect(jsonPath("$.data[0].casadas").value(1));
    }

    private static byte[] pdf(String... lineas) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {
                c.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);
                float y = 750;
                for (String linea : lineas) {
                    c.beginText();
                    c.newLineAtOffset(40, y);
                    c.showText(linea);
                    c.endText();
                    y -= 14;
                }
            }
            doc.save(salida);
            return salida.toByteArray();
        }
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
        peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (!(peticion instanceof org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder)) {
            peticion.contentType(MediaType.APPLICATION_JSON);
        }
        return mockMvc.perform(peticion);
    }
}
