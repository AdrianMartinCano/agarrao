package dev.pimon.listillo.gasolina.estaciones;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deja los nombres de municipio y provincia del Ministerio como los escribiría una persona.
 *
 * El origen usa el formato de los registros oficiales, que nadie teclea en un buscador:
 *
 *   "Coruña (A)"                   →  "A Coruña"
 *   "Palmas de Gran Canaria (Las)" →  "Las Palmas de Gran Canaria"
 *   "CASTELLÓN / CASTELLÓ"         →  "Castellón / Castelló"
 *   "ÁVILA"                        →  "Ávila"
 *
 * Esto no es solo cosmética: como la búsqueda va contra el nombre que se muestra, mover
 * el artículo al principio es lo que hace que escribir "a coru" encuentre A Coruña. Antes
 * el nombre indexado era "Coruña (A)", que no empieza ni contiene "a coru", así que la
 * ciudad era literalmente inencontrable escribiéndola bien.
 */
final class NombresLugares {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    /**
     * Artículos que el registro oficial manda al final entre paréntesis. Están los
     * castellanos, los gallegos (A, O, As, Os) y los catalanes/baleares (Es, Sa, S').
     */
    private static final Pattern ARTICULO_AL_FINAL = Pattern.compile(
            "^(.+?)[,\\s]*\\((A|As|O|Os|El|La|Los|Las|L'|Es|Els|Sa|Ses|S')\\)$",
            Pattern.CASE_INSENSITIVE);

    /** Palabras que dentro de un nombre van en minúscula ("Palmas de Gran Canaria"). */
    private static final List<String> MINUSCULAS =
            List.of("de", "del", "la", "las", "los", "el", "y", "e", "i", "da", "do", "dos", "das");

    private NombresLugares() {
    }

    /**
     * Municipio listo para mostrar y para buscar.
     * Ya viene bien escrito del origen, así que solo hay que recolocar el artículo.
     */
    static String municipio(String nombre) {
        return moverArticuloAlPrincipio(nombre.trim());
    }

    /**
     * Provincia lista para mostrar.
     * Llega toda en mayúsculas ("ÁVILA", "CORUÑA (A)"), así que además hay que recapitalizar.
     */
    static String provincia(String nombre) {
        return moverArticuloAlPrincipio(capitalizar(nombre.trim()));
    }

    /** "Coruña (A)" → "A Coruña". Si no hay artículo al final, lo deja como está. */
    private static String moverArticuloAlPrincipio(String nombre) {
        Matcher coincidencia = ARTICULO_AL_FINAL.matcher(nombre);
        if (!coincidencia.matches()) {
            return nombre;
        }

        String resto    = coincidencia.group(1).trim();
        String articulo = coincidencia.group(2);

        // Los apostrofados se pegan a la palabra siguiente: "L'Hospitalet", no "L' Hospitalet".
        String separador = articulo.endsWith("'") ? "" : " ";

        return capitalizarPalabra(articulo) + separador + resto;
    }

    /** "CASTELLÓN / CASTELLÓ" → "Castellón / Castelló", respetando barras y guiones. */
    private static String capitalizar(String texto) {
        // Se separa conservando los delimitadores para no perder las barras ni los guiones
        // de los nombres bilingües.
        String[] trozos = texto.split("(?<=[\\s/\\-])|(?=[\\s/\\-])");
        StringBuilder resultado = new StringBuilder(texto.length());
        boolean primeraPalabra = true;

        for (String trozo : trozos) {
            if (trozo.isBlank() || !Character.isLetter(trozo.charAt(0))) {
                resultado.append(trozo);
                continue;
            }

            String minuscula = trozo.toLowerCase(ESPANOL);
            // La primera palabra va SIEMPRE en mayúscula, aunque sea de las que no se
            // capitalizan: "LA RIOJA" es "La Rioja", no "la Rioja".
            resultado.append(!primeraPalabra && MINUSCULAS.contains(minuscula)
                    ? minuscula
                    : capitalizarPalabra(trozo));
            primeraPalabra = false;
        }

        return resultado.toString();
    }

    private static String capitalizarPalabra(String palabra) {
        if (palabra.isEmpty()) {
            return palabra;
        }
        return Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1).toLowerCase(ESPANOL);
    }
}
