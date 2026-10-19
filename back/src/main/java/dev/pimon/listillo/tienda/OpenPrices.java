package dev.pimon.listillo.tienda;

import com.fasterxml.jackson.databind.JsonNode;
import dev.pimon.listillo.producto.off.OffProperties;
import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La API de Open Prices. No filtra precios por país: primero se buscan los locales de España
 * con algún precio ({@code /api/v1/locations?osm_address_country__like=…}) y se reconoce su
 * cadena por la marca o el nombre de OpenStreetMap («Carrefour Express», «Dia&Go»); después
 * se piden los precios de esos locales ({@code /api/v1/prices?location_id__in=…}).
 *
 * En España hay poco (unos mil precios en oct 2026, la mitad de cadenas sin web leíble), así
 * que son pocas peticiones.
 */
@Component
public class OpenPrices {

    /** Cómo escribe OpenStreetMap el país, según el idioma de la zona. */
    static final List<String> PAISES = List.of("España", "Espanya", "Spain", "Espainia", "Espanha");
    private static final int POR_PAGINA = 100;
    private static final int LOCALES_POR_PETICION = 40;
    private static final int MAXIMO_PAGINAS = 100;
    private static final int NOMBRE_MAXIMO = 160;

    /** Un precio leído: de qué cadena, de qué día y el producto. */
    public record PrecioAbierto(Cadena cadena, LocalDate fecha, ProductoWeb producto, String categoriasOff) {
    }

    private final RestClient http;
    private final OpenPricesProperties props;

    public OpenPrices(OpenPricesProperties props, OffProperties off, ObjectProvider<BuildProperties> build) {
        this.props = props;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(props.espera());
        fabrica.setReadTimeout(props.espera());
        String version = build.getIfAvailable() != null ? build.getIfAvailable().getVersion() : "dev";
        this.http = RestClient.builder()
                .baseUrl(props.url())
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.USER_AGENT, off.userAgent(version))
                .build();
    }

    /** Los locales de España con precios, con su cadena (los de cadenas que no conocemos, fuera). */
    public Map<Long, Cadena> locales() {
        Map<Long, Cadena> locales = new HashMap<>();
        for (String pais : PAISES) {
            for (int pagina = 1; pagina <= MAXIMO_PAGINAS; pagina++) {
                JsonNode respuesta = pedir("/api/v1/locations?osm_address_country__like={pais}&price_count__gte=1"
                        + "&size={size}&page={page}", pais, POR_PAGINA, pagina);
                for (JsonNode l : respuesta.path("items")) {
                    cadenaDe(l).ifPresent(c -> locales.put(l.path("id").asLong(), c));
                }
                if (pagina >= respuesta.path("pages").asInt(0)) {
                    break;
                }
            }
        }
        return locales;
    }

    /** Los precios de esos locales desde ese día. */
    public List<PrecioAbierto> precios(Map<Long, Cadena> locales, LocalDate desde) {
        List<Long> ids = new ArrayList<>(locales.keySet());
        List<PrecioAbierto> leidos = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += LOCALES_POR_PETICION) {
            String grupo = ids.subList(i, Math.min(i + LOCALES_POR_PETICION, ids.size())).stream()
                    .map(String::valueOf).collect(Collectors.joining(","));
            for (int pagina = 1; pagina <= MAXIMO_PAGINAS; pagina++) {
                JsonNode respuesta = pedir("/api/v1/prices?location_id__in={ids}&date__gte={desde}&currency=EUR"
                        + "&size={size}&page={page}", grupo, desde, POR_PAGINA, pagina);
                leidos.addAll(leerPrecios(respuesta, locales));
                if (pagina >= respuesta.path("pages").asInt(0)) {
                    break;
                }
            }
        }
        return leidos;
    }

    private JsonNode pedir(String uri, Object... variables) {
        try {
            Thread.sleep(props.pausa());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Importación interrumpida", e);
        }
        JsonNode respuesta = http.get().uri(uri, variables).retrieve().body(JsonNode.class);
        if (respuesta == null) {
            throw new IllegalStateException("Open Prices no ha devuelto nada");
        }
        return respuesta;
    }

    /** La cadena de un local de España, por su marca en OpenStreetMap o, si no tiene, su nombre. */
    static Optional<Cadena> cadenaDe(JsonNode local) {
        if (!"ES".equals(local.path("osm_address_country_code").asText())) {
            return Optional.empty();
        }
        String marca = local.path("osm_brand").asText("");
        String nombre = marca.isBlank() ? local.path("osm_name").asText("") : marca;
        if (nombre.isBlank()) {
            return Optional.empty();
        }
        Cadena cadena = Cadena.detectar(List.of(nombre));
        return cadena == Cadena.OTRA ? Optional.empty() : Optional.of(cadena);
    }

    /** Una página de precios: solo los de un producto con código, en euros y con nombre. */
    static List<PrecioAbierto> leerPrecios(JsonNode pagina, Map<Long, Cadena> locales) {
        List<PrecioAbierto> leidos = new ArrayList<>();
        for (JsonNode p : pagina.path("items")) {
            Cadena cadena = locales.get(p.path("location_id").asLong());
            JsonNode producto = p.path("product");
            String codigo = p.path("product_code").asText("");
            String nombre = producto.path("product_name").asText("").strip();
            BigDecimal precio = p.path("price").decimalValue();
            if (cadena == null || !"PRODUCT".equals(p.path("type").asText()) || !"EUR".equals(p.path("currency").asText())
                    || codigo.isBlank() || codigo.length() > 40 || nombre.isBlank() || precio.signum() <= 0) {
                continue;
            }
            LocalDate fecha;
            try {
                fecha = LocalDate.parse(p.path("date").asText());
            } catch (RuntimeException e) {
                continue;
            }
            producto(codigo, nombre, producto, precio.setScale(2, RoundingMode.HALF_UP))
                    .ifPresent(w -> leidos.add(new PrecioAbierto(cadena, fecha, w, categorias(producto))));
        }
        return leidos;
    }

    private static Optional<ProductoWeb> producto(String codigo, String nombre, JsonNode producto, BigDecimal precio) {
        Medida medida = medida(producto.path("product_quantity").asDouble(0),
                producto.path("product_quantity_unit").asText(""));
        if (medida.tamano().signum() <= 0 || precio.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal porUnidad = precio.divide(medida.tamano(), 3, RoundingMode.HALF_UP);
        if (porUnidad.signum() <= 0) {
            return Optional.empty();
        }
        String completo = conMarca(nombre, producto.path("brands").asText(""));
        return Optional.of(new ProductoWeb(codigo, completo, medida.formato(), precio, medida.tamano(), medida.unidad(),
                porUnidad, null, texto(producto.path("image_url")),
                "https://prices.openfoodfacts.org/products/" + codigo));
    }

    /** Lo que trae el envase, en kilos, litros o piezas. Sin dato, una pieza. */
    record Medida(BigDecimal tamano, Unidad unidad, String formato) {
    }

    static Medida medida(double cantidad, String unidad) {
        String u = unidad.strip().toLowerCase(Locale.ROOT);
        String formato = cantidad > 0 && !u.isBlank()
                ? BigDecimal.valueOf(cantidad).stripTrailingZeros().toPlainString() + " " + u : null;
        if (cantidad > 0) {
            BigDecimal c = BigDecimal.valueOf(cantidad);
            switch (u) {
                case "g", "gr" -> {
                    return new Medida(c.movePointLeft(3).setScale(3, RoundingMode.HALF_UP), Unidad.KG, formato);
                }
                case "kg" -> {
                    return new Medida(c.setScale(3, RoundingMode.HALF_UP), Unidad.KG, formato);
                }
                case "ml" -> {
                    return new Medida(c.movePointLeft(3).setScale(3, RoundingMode.HALF_UP), Unidad.L, formato);
                }
                case "cl" -> {
                    return new Medida(c.movePointLeft(2).setScale(3, RoundingMode.HALF_UP), Unidad.L, formato);
                }
                case "l" -> {
                    return new Medida(c.setScale(3, RoundingMode.HALF_UP), Unidad.L, formato);
                }
                default -> {
                    // «uds», «unidades»… o una unidad rara: se cuenta como una pieza.
                }
            }
        }
        return new Medida(BigDecimal.ONE.setScale(3), Unidad.UD, formato);
    }

    /** «Ensalada César» + «Eliges, IFA RETAIL S.A» → «Ensalada César Eliges». */
    static String conMarca(String nombre, String marcas) {
        nombre = nombre.replaceAll("[.\\s]+$", "");
        String marca = marcas.split(",")[0].strip();
        String completo = marca.isBlank() || nombre.toLowerCase(Locale.ROOT).contains(marca.toLowerCase(Locale.ROOT))
                ? nombre : nombre + " " + marca;
        completo = completo.replaceAll("[.\\s]+$", "");
        return completo.length() > NOMBRE_MAXIMO ? completo.substring(0, NOMBRE_MAXIMO) : completo;
    }

    private static String categorias(JsonNode producto) {
        List<String> etiquetas = new ArrayList<>();
        producto.path("categories_tags").forEach(c -> etiquetas.add(c.asText()));
        return etiquetas.isEmpty() ? null : String.join(",", etiquetas);
    }

    private static String texto(JsonNode nodo) {
        String t = nodo.asText("");
        return t.isBlank() || t.length() > 500 ? null : t;
    }
}
