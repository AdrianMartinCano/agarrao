package dev.pimon.listillo.producto.off;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pimon.listillo.producto.CatalogoProductos;
import dev.pimon.listillo.producto.DatosProducto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Open Food Facts, API v2, solo los campos que se usan. Responde 404 (o {@code status: 0})
 * cuando no conoce el código.
 */
@Component
public class OpenFoodFacts implements CatalogoProductos {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFacts.class);
    private static final String CAMPOS =
            "product_name,product_name_es,brands,quantity,categories_tags,image_front_small_url";
    /** Las más generales primero; con cinco se sabe de qué va sin guardar la lista entera. */
    private static final int CATEGORIAS = 5;

    private final RestClient http;
    private final LimitePorMinuto limite;

    public OpenFoodFacts(OffProperties props, ObjectProvider<BuildProperties> build) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(props.espera());
        fabrica.setReadTimeout(props.espera());
        String version = build.getIfAvailable() != null ? build.getIfAvailable().getVersion() : "dev";
        this.http = RestClient.builder()
                .baseUrl(props.url())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent(version))
                .build();
        this.limite = new LimitePorMinuto(props.porMinuto(), Clock.systemUTC());
    }

    @Override
    public Optional<DatosProducto> buscar(String ean) {
        if (!limite.pedir()) {
            throw new CatalogoNoDisponible("Límite de consultas a Open Food Facts alcanzado", null);
        }
        try {
            Respuesta respuesta = http.get()
                    .uri("/api/v2/product/{ean}.json?fields={campos}&lc=es", ean, CAMPOS)
                    .retrieve()
                    // 404 = no lo conoce: no es un error.
                    .onStatus(estado -> estado.value() == 404, (peticion, r) -> { })
                    .body(Respuesta.class);
            if (respuesta == null || respuesta.status() != 1 || respuesta.product() == null) {
                return Optional.empty();
            }
            return respuesta.product().aDatos();
        } catch (RestClientException e) {
            log.warn("Open Food Facts no responde para {}: {}", ean, e.getMessage());
            throw new CatalogoNoDisponible("Open Food Facts no responde", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(int status, Producto product) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Producto(
            @JsonProperty("product_name") String nombre,
            @JsonProperty("product_name_es") String nombreEs,
            @JsonProperty("brands") String marcas,
            @JsonProperty("quantity") String cantidad,
            @JsonProperty("categories_tags") List<String> categorias,
            @JsonProperty("image_front_small_url") String imagen
    ) {
        /** Sin nombre no sirve para la lista: cuenta como que no lo conoce. */
        Optional<DatosProducto> aDatos() {
            String elNombre = primero(nombreEs, nombre);
            if (elNombre == null) {
                return Optional.empty();
            }
            // «Hacendado, Mercadona» → la primera es la marca que va en el envase.
            String marca = marcas == null ? null : primero(marcas.split(",")[0]);
            String cats = categorias == null || categorias.isEmpty() ? null
                    : String.join(",", categorias.subList(0, Math.min(CATEGORIAS, categorias.size())));
            return Optional.of(new DatosProducto(
                    recortar(elNombre, 120), recortar(marca, 80), recortar(limpiarCantidad(primero(cantidad)), 40),
                    recortar(imagen, 500), recortar(cats, 500)));
        }

        /** «400 g e» o «400 g ℮»: el signo de cantidad estimada de la etiqueta sobra aquí. */
        static String limpiarCantidad(String cantidad) {
            return cantidad == null ? null : cantidad.replaceAll("[\\s\\u00a0]+(e|℮)$", "").strip();
        }

        private static String primero(String... valores) {
            for (String v : valores) {
                if (v != null && !v.isBlank()) {
                    return v.strip();
                }
            }
            return null;
        }

        private static String recortar(String valor, int largo) {
            return valor == null || valor.length() <= largo ? valor : valor.substring(0, largo);
        }
    }
}
