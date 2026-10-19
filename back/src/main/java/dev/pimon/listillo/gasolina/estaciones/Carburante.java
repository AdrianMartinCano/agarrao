package dev.pimon.listillo.gasolina.estaciones;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tipos de carburante que publica el Ministerio.
 *
 * Cada valor ata tres cosas que no coinciden entre sí:
 *   - el nombre de la constante, que es lo que viaja por nuestra API (GASOLINA_95_E5);
 *   - {@link #campoMinetur()}, el nombre EXACTO del campo en el JSON del Ministerio,
 *     con sus espacios y sus tildes ("Precio Gasolina 95 E5");
 *   - {@link #etiqueta()}, el texto que se le enseña al usuario.
 *
 * El nombre del campo externo se queda encerrado aquí a propósito: si el Ministerio
 * renombra una columna, se cambia en esta línea y no en media docena de sitios.
 *
 * HABITUALES. Están los 23 tipos que publica el Ministerio, pero solo unos pocos se
 * ofrecen para buscar (ver {@link #habituales()}). El resto —metanol, amoniaco, biogás
 * licuado, gasolina E85…— no los reposta prácticamente nadie y llenaban el desplegable
 * de ruido, obligando a buscar "gasolina 95" entre veintitrés opciones.
 *
 * Se conservan TODOS en el enum, no se borran: sus precios se siguen leyendo y guardando
 * para cada estación. Lo que se recorta es lo que se ofrece en el buscador, no los datos.
 * Quien llame a la API directamente puede seguir pidiendo cualquiera de ellos.
 */
public enum Carburante {

    // ── Los que se ofrecen en el buscador ── //
    GASOLINA_95_E5("Precio Gasolina 95 E5", "Gasolina 95", true),
    GASOLINA_98_E5("Precio Gasolina 98 E5", "Gasolina 98", true),
    GASOLEO_A("Precio Gasoleo A", "Diésel", true),
    GLP("Precio Gases licuados del petróleo", "GLP", true),
    ADBLUE("Precio Adblue", "AdBlue", true),

    // ── El resto: se leen y se guardan, pero no salen en el desplegable ── //
    GASOLINA_95_E5_PREMIUM("Precio Gasolina 95 E5 Premium", "Gasolina 95 Premium", false),
    GASOLINA_95_E10("Precio Gasolina 95 E10", "Gasolina 95 E10", false),
    GASOLINA_95_E25("Precio Gasolina 95 E25", "Gasolina 95 E25", false),
    GASOLINA_95_E85("Precio Gasolina 95 E85", "Gasolina 95 E85", false),
    GASOLINA_98_E10("Precio Gasolina 98 E10", "Gasolina 98 E10", false),
    GASOLINA_RENOVABLE("Precio Gasolina Renovable", "Gasolina renovable", false),
    GASOLEO_B("Precio Gasoleo B", "Gasóleo B (agrícola)", false),
    GASOLEO_PREMIUM("Precio Gasoleo Premium", "Diésel Premium", false),
    DIESEL_RENOVABLE("Precio Diésel Renovable", "Diésel renovable", false),
    BIODIESEL("Precio Biodiesel", "Biodiésel", false),
    BIOETANOL("Precio Bioetanol", "Bioetanol", false),
    GNC("Precio Gas Natural Comprimido", "Gas natural comprimido", false),
    GNL("Precio Gas Natural Licuado", "Gas natural licuado", false),
    BIOGAS_COMPRIMIDO("Precio Biogas Natural Comprimido", "Biogás comprimido", false),
    BIOGAS_LICUADO("Precio Biogas Natural Licuado", "Biogás licuado", false),
    HIDROGENO("Precio Hidrogeno", "Hidrógeno", false),
    AMONIACO("Precio Amoniaco", "Amoniaco", false),
    METANOL("Precio Metanol", "Metanol", false);

    private static final Map<String, Carburante> POR_CAMPO = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Carburante::campoMinetur, Function.identity()));

    private static final List<Carburante> HABITUALES = Arrays.stream(values())
            .filter(Carburante::habitual)
            .toList();

    private final String  campoMinetur;
    private final String  etiqueta;
    private final boolean habitual;

    Carburante(String campoMinetur, String etiqueta, boolean habitual) {
        this.campoMinetur = campoMinetur;
        this.etiqueta     = etiqueta;
        this.habitual     = habitual;
    }

    public String campoMinetur() {
        return campoMinetur;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Si se ofrece en el desplegable del buscador. */
    public boolean habitual() {
        return habitual;
    }

    /**
     * Los carburantes que se ofrecen para buscar, en el orden en que se declaran (que es
     * el orden en que se enseñan: primero los que más se reponstan).
     */
    public static List<Carburante> habituales() {
        return HABITUALES;
    }

    /**
     * Busca el carburante por el nombre del campo del JSON del Ministerio.
     * Devuelve vacío si es un campo que no conocemos: si mañana publican un carburante
     * nuevo, se ignora ese precio en vez de reventar la carga de las 11.000 estaciones.
     */
    public static Optional<Carburante> porCampoMinetur(String campo) {
        return Optional.ofNullable(POR_CAMPO.get(campo));
    }
}
