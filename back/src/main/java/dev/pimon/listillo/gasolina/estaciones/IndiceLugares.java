package dev.pimon.listillo.gasolina.estaciones;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Los municipios y códigos postales que se pueden buscar, sacados del catálogo.
 *
 * Se construye UNA vez por refresco del catálogo (cada media hora) y no en cada
 * búsqueda: recorrer once mil estaciones y agrupar por municipio en cada pulsación del
 * teclado sería absurdo. Es inmutable, así que se puede consultar desde varias
 * peticiones a la vez sin sincronizar nada.
 */
public final class IndiceLugares {

    /** Cuántas sugerencias se devuelven como mucho. Una lista más larga no se lee. */
    private static final int MAXIMO_SUGERENCIAS = 8;

    private final List<Lugar> lugares;
    private final List<String> clavesNormalizadas;

    private IndiceLugares(List<Lugar> lugares) {
        this.lugares = lugares;
        // Se normaliza una sola vez, al construir: si se hiciera en cada comparación,
        // cada búsqueda haría miles de normalizaciones de cadenas.
        this.clavesNormalizadas = lugares.stream().map(l -> normalizar(l.nombre())).toList();
    }

    public static IndiceLugares vacio() {
        return new IndiceLugares(List.of());
    }

    /**
     * Agrupa las estaciones en municipios y códigos postales.
     *
     * La coordenada de cada lugar es la media de las de sus estaciones. No es el centro
     * geográfico del municipio, sino el centro de sus gasolineras, que para lo que se usa
     * (buscar las de alrededor) es incluso mejor: cae donde de verdad hay dónde repostar.
     */
    public static IndiceLugares de(List<Estacion> estaciones) {
        Map<String, Acumulador> municipios = new LinkedHashMap<>();
        Map<String, Acumulador> codigosPostales = new LinkedHashMap<>();

        for (Estacion estacion : estaciones) {
            // Los nombres se arreglan aquí, una vez por estación, y no al mostrarlos:
            // el nombre corregido es TAMBIÉN por el que se busca ("A Coruña", no
            // "Coruña (A)"), así que tiene que ser el que entra en el índice.
            String municipio = NombresLugares.municipio(estacion.municipio());
            String provincia = NombresLugares.provincia(estacion.provincia());

            if (!municipio.isBlank()) {
                // Municipio + provincia: hay muchos nombres repetidos por toda España
                // (Villanueva, Alcalá…) y mezclarlos daría un punto medio absurdo, en
                // algún lugar entre las dos provincias.
                municipios
                        .computeIfAbsent(
                                municipio + "|" + provincia,
                                clave -> new Acumulador(municipio, provincia, Lugar.TipoLugar.MUNICIPIO))
                        .anadir(estacion);
            }

            // Solo códigos postales bien formados: el origen trae alguno vacío o a medias.
            if (estacion.codigoPostal().matches("\\d{5}")) {
                codigosPostales
                        .computeIfAbsent(
                                estacion.codigoPostal(),
                                clave -> new Acumulador(estacion.codigoPostal(),
                                        municipio + " · " + provincia,
                                        Lugar.TipoLugar.CODIGO_POSTAL))
                        .anadir(estacion);
            }
        }

        List<Lugar> todos = new ArrayList<>(municipios.size() + codigosPostales.size());
        municipios.values().forEach(a -> todos.add(a.aLugar()));
        codigosPostales.values().forEach(a -> todos.add(a.aLugar()));

        return new IndiceLugares(List.copyOf(todos));
    }

    /**
     * Busca sitios cuyo nombre encaje con el texto escrito.
     *
     * Se ignoran tildes y mayúsculas, porque nadie escribe "Alcalá de Henares" con la
     * tilde puesta en un buscador. Y los que EMPIEZAN por lo escrito van antes que los
     * que solo lo contienen: quien teclea "get" busca Getafe, no "Puente de Getafe".
     */
    public List<Lugar> buscar(String texto) {
        String consulta = normalizar(texto);
        if (consulta.length() < 2) {
            // Con una sola letra saldrían cientos de sitios y ninguno sería el bueno.
            return List.of();
        }

        record Candidato(Lugar lugar, boolean empiezaPor) {
        }

        List<Candidato> candidatos = new ArrayList<>();
        for (int i = 0; i < lugares.size(); i++) {
            String clave = clavesNormalizadas.get(i);
            if (clave.startsWith(consulta)) {
                candidatos.add(new Candidato(lugares.get(i), true));
            } else if (clave.contains(consulta)) {
                candidatos.add(new Candidato(lugares.get(i), false));
            }
        }

        return candidatos.stream()
                .sorted(Comparator
                        .comparing((Candidato c) -> !c.empiezaPor())          // los que empiezan por, primero
                        .thenComparing(c -> -c.lugar().estaciones())          // los sitios grandes antes
                        .thenComparing(c -> c.lugar().nombre()))              // y a igualdad, alfabético
                .limit(MAXIMO_SUGERENCIAS)
                .map(Candidato::lugar)
                .toList();
    }

    public int tamano() {
        return lugares.size();
    }

    /** Pasa a minúsculas y quita las tildes, para que "alcala" encuentre "Alcalá". */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    /** Va sumando coordenadas para sacar la media al final, sin guardar todas las estaciones. */
    private static final class Acumulador {
        private final String nombre;
        private final String detalle;
        private final Lugar.TipoLugar tipo;
        private double sumaLatitud;
        private double sumaLongitud;
        private int cuantas;

        Acumulador(String nombre, String detalle, Lugar.TipoLugar tipo) {
            this.nombre  = nombre;
            this.detalle = detalle;
            this.tipo    = tipo;
        }

        void anadir(Estacion estacion) {
            sumaLatitud  += estacion.latitud();
            sumaLongitud += estacion.longitud();
            cuantas++;
        }

        Lugar aLugar() {
            return new Lugar(nombre, detalle, tipo, sumaLatitud / cuantas, sumaLongitud / cuantas, cuantas);
        }
    }
}
