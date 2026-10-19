package dev.pimon.listillo.ticket.lectura;

import dev.pimon.listillo.ticket.Cadena;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Del texto de un ticket (sacado de un PDF o de una foto) a tienda, fecha, total y líneas.
 *
 * Reglas generales, no una plantilla por cadena: los tickets españoles comparten casi
 * siempre las mismas formas de línea, y una foto leída con OCR no respeta plantillas.
 * <pre>
 * 1 LECHE SEMI HACEND 6X1L     5,34      cantidad delante, importe al final
 * 2 YOGUR GRIEGO     0,95  1,90         cantidad, precio unitario e importe
 * 1 PLATANO                              artículo a peso: sin precio en su línea…
 * 0,734 kg  1,99 €/kg   1,46             …y el peso en la siguiente
 * PAN DE MOLDE        1,19 A            letra de IVA al final (Lidl)
 * 2 x 0,99                               cantidad por precio en la línea de abajo
 * NAPOLITANA                             Dia: el nombre partido en dos líneas…
 * MIXTA 2 ud 0,75 € 1,50 € A             …y cantidad con unidad, precio e importe con €
 * </pre>
 * El total puede ir antes que los artículos (Dia pone el resumen arriba): entonces se leen
 * desde la cabecera de la tabla hasta el siguiente total.
 * Lo que no se entiende no se inventa: se deja fuera y, si las líneas no suman el total,
 * se avisa para que la persona lo repase.
 */
public final class InterpreteTicket {

    private static final String IMPORTE = "(-?\\d{1,4}[.,]\\d{2})";
    private static final Pattern FECHA = Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4}|\\d{2})\\b");
    private static final Pattern TOTAL = Pattern.compile(
            "^(?:IMPORTE\\s+)?TOTAL(?!\\s*(?:ART|IVA|UNID|DESC|AHORR|SIN\\b))\\b.*?" + IMPORTE + "\\s*(?:€|EUR)?$");
    private static final Pattern CABECERA_TABLA = Pattern.compile("DESCRIPCI|P\\.?\\s*UNIT|ARTICULO\\s+.*IMPORTE");
    private static final Pattern PESO = Pattern.compile(
            // Tras «/» vale cualquier cosa corta: el OCR lee «€/kg» como «EUR/KKE» o «€/K9».
            "^(\\d{1,3}[.,]\\d{1,3})\\s*(KG|G|L)\\b\\s*[X*]?\\s*" + IMPORTE + "\\s*(?:€|EUR)?\\s*/\\s*[A-Z0-9]{1,4}\\s*" + IMPORTE + "?\\s*[A-E]?$");
    /** Dia: «MIXTA 2 ud 0,75 € 1,50 € A» o «PLATANO 0,734 kg 1,99 € 1,46 € A» (el nombre puede empezar arriba). */
    private static final Pattern CON_UNIDAD = Pattern.compile(
            "^(.*?)\\s*(\\d{1,3}(?:[.,]\\d{1,3})?)\\s*(UDS?|KG|G|L)\\s+" + IMPORTE + "\\s*(?:€|EUR)\\s+" + IMPORTE
                    + "\\s*(?:€|EUR)\\s*[A-E]?$");
    private static final Pattern POR_UNIDADES = Pattern.compile(
            "^(\\d{1,3})\\s*[X*]\\s*" + IMPORTE + "\\s*(?:€|EUR)?(?:\\s+" + IMPORTE + ")?\\s*[A-E]?$");
    private static final Pattern CON_UNITARIO = Pattern.compile(
            "^(?:(\\d{1,3})\\s+)?(.*[A-Z].*?)\\s+" + IMPORTE + "\\s+" + IMPORTE + "\\s*(?:€|[A-E])?$");
    private static final Pattern SOLO_IMPORTE = Pattern.compile(
            "^(?:(\\d{1,3})\\s+)?(.*[A-Z].*?)\\s+" + IMPORTE + "\\s*(?:€|[A-E])?$");
    private static final Pattern SIN_PRECIO = Pattern.compile("^(?:(\\d{1,3})\\s+)?(\\p{Lu}[\\p{Lu}0-9 .,%/'-]{2,})$");
    /** Líneas de cabecera, pie y pago que no son artículos. */
    private static final Pattern IGNORAR = Pattern.compile(
            "N\\.?I\\.?F|C\\.?I\\.?F|TEL[EÉ]?F|FACTURA|^C/|CALLE|AVDA|^OP[:.]|TARJETA|VISA|MASTERCARD|"
                    + "EFECTIVO|ENTREGAD|\\bCAMBIO\\b|\\bIVA\\b|BASE IMP|\\bCUOTA\\b|DEVOLUCI|GRACIAS|HORARIO|WWW\\.|"
                    // Palabras enteras: «OLIVA» lleva «IVA» dentro y es un artículo.
                    + "\\bCAJA\\s*\\d|\\bTIENDA\\b|S\\.A\\.|S\\.L\\.|\\bAUT[:.]|\\bAID[:.]|\\bARC[:.]|LEIDA|CONTACTLESS|IMPORTE:");

    private InterpreteTicket() {
    }

    public static TicketLeido leer(String texto) {
        List<String> lineas = Arrays.stream(texto.split("\\R"))
                .map(InterpreteTicket::limpiar)
                .filter(l -> !l.isEmpty())
                .toList();

        Cadena cadena = Cadena.detectar(lineas);
        LocalDate fecha = fecha(lineas);

        int finArticulos = lineas.size();
        BigDecimal total = null;
        for (int i = 0; i < lineas.size(); i++) {
            Matcher m = TOTAL.matcher(lineas.get(i));
            if (m.find()) {
                total = importe(m.group(1));
                finArticulos = i;
                break;
            }
        }
        int inicio = 0;
        int tabla = -1;
        for (int i = 0; i < lineas.size(); i++) {
            if (CABECERA_TABLA.matcher(lineas.get(i)).find()) {
                tabla = i;
                break;
            }
        }
        if (tabla >= 0 && tabla < finArticulos) {
            inicio = tabla + 1;
        } else if (tabla > finArticulos) {
            // Resumen arriba y tabla debajo (Dia): de la cabecera al siguiente total.
            inicio = tabla + 1;
            finArticulos = lineas.size();
            for (int i = inicio; i < lineas.size(); i++) {
                if (TOTAL.matcher(lineas.get(i)).find()) {
                    finArticulos = i;
                    break;
                }
            }
        }

        List<LineaLeida> leidas = articulos(lineas.subList(inicio, finArticulos));
        List<String> avisos = avisos(leidas, total);
        return new TicketLeido(cadena, cabecera(lineas, cadena), fecha, total, leidas, avisos);
    }

    private static List<LineaLeida> articulos(List<String> lineas) {
        List<LineaLeida> leidas = new ArrayList<>();
        Pendiente pendiente = null;

        for (String linea : lineas) {
            if (IGNORAR.matcher(linea).find() || FECHA.matcher(linea).find()) {
                continue;
            }
            Matcher m;
            if ((m = CON_UNIDAD.matcher(linea)).find()) {
                String nombre = ((pendiente != null ? pendiente.texto + " " : "") + m.group(1)).strip();
                pendiente = null;
                if (!conLetras(nombre)) {
                    continue;
                }
                Unidad unidad = switch (m.group(3)) {
                    case "KG", "G" -> Unidad.KG;
                    case "L" -> Unidad.L;
                    default -> Unidad.UD;
                };
                BigDecimal cantidad = cantidad(m.group(2), m.group(3));
                BigDecimal precio = importe(m.group(4));
                BigDecimal total = importe(m.group(5));
                String texto = texto(nombre);
                // Una devolución en caja (el mismo artículo en negativo) anula la línea de antes.
                if (total.signum() < 0 && !leidas.isEmpty() && leidas.getLast().texto().equals(texto)
                        && leidas.getLast().importe().compareTo(total.negate()) == 0) {
                    leidas.removeLast();
                    continue;
                }
                leidas.add(new LineaLeida(texto, cantidad, unidad, precio, total));
                continue;
            }
            if ((m = PESO.matcher(linea)).find()) {
                BigDecimal cantidad = cantidad(m.group(1), m.group(2));
                Unidad unidad = "L".equals(m.group(2)) ? Unidad.L : Unidad.KG;
                BigDecimal precio = importe(m.group(3));
                BigDecimal total = m.group(4) != null ? importe(m.group(4)) : precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
                if (pendiente != null) {
                    leidas.add(new LineaLeida(pendiente.texto, cantidad, unidad, precio, total));
                    pendiente = null;
                } else if (!leidas.isEmpty()) {
                    // Lidl: el artículo ya salió con su importe y el peso va debajo.
                    LineaLeida ultima = leidas.removeLast();
                    leidas.add(new LineaLeida(ultima.texto(), cantidad, unidad, precio, ultima.importe()));
                }
                continue;
            }
            if ((m = POR_UNIDADES.matcher(linea)).find()) {
                BigDecimal cantidad = new BigDecimal(m.group(1));
                BigDecimal precio = importe(m.group(2));
                BigDecimal total = m.group(3) != null ? importe(m.group(3)) : precio.multiply(cantidad);
                if (pendiente != null) {
                    leidas.add(new LineaLeida(pendiente.texto, cantidad, Unidad.UD, precio, total));
                    pendiente = null;
                } else if (!leidas.isEmpty()) {
                    LineaLeida ultima = leidas.removeLast();
                    leidas.add(new LineaLeida(ultima.texto(), cantidad, Unidad.UD, precio, ultima.importe()));
                }
                continue;
            }
            if ((m = CON_UNITARIO.matcher(linea)).find() && conLetras(m.group(2))) {
                BigDecimal cantidad = m.group(1) != null ? new BigDecimal(m.group(1)) : BigDecimal.ONE;
                leidas.add(new LineaLeida(texto(m.group(2)), cantidad, Unidad.UD, importe(m.group(3)), importe(m.group(4))));
                pendiente = null;
                continue;
            }
            if ((m = SOLO_IMPORTE.matcher(linea)).find() && conLetras(m.group(2))) {
                BigDecimal cantidad = m.group(1) != null ? new BigDecimal(m.group(1)) : BigDecimal.ONE;
                BigDecimal total = importe(m.group(3));
                BigDecimal precio = total.divide(cantidad, 3, RoundingMode.HALF_UP).stripTrailingZeros();
                leidas.add(new LineaLeida(texto(m.group(2)), cantidad, Unidad.UD, precio, total));
                pendiente = null;
                continue;
            }
            if ((m = SIN_PRECIO.matcher(linea)).find() && conLetras(m.group(2))) {
                // Un artículo a peso (su precio llega en la línea siguiente) o ruido.
                pendiente = new Pendiente(texto(m.group(2)));
            }
        }
        return leidas;
    }

    private static List<String> avisos(List<LineaLeida> leidas, BigDecimal total) {
        List<String> avisos = new ArrayList<>();
        if (leidas.isEmpty()) {
            avisos.add("No hemos encontrado artículos en este ticket. Añádelos a mano o prueba con otra foto.");
            return avisos;
        }
        if (total != null) {
            BigDecimal suma = leidas.stream().map(LineaLeida::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (suma.subtract(total).abs().compareTo(new BigDecimal("0.02")) > 0) {
                avisos.add("Las líneas suman " + euros(suma) + " y el total del ticket es " + euros(total)
                        + ": revisa que no falte ni sobre ninguna.");
            }
        } else {
            avisos.add("No hemos encontrado el total del ticket.");
        }
        return avisos;
    }

    private static LocalDate fecha(List<String> lineas) {
        for (String linea : lineas) {
            Matcher m = FECHA.matcher(linea);
            while (m.find()) {
                try {
                    int anio = Integer.parseInt(m.group(3));
                    LocalDate f = LocalDate.of(anio < 100 ? 2000 + anio : anio,
                            Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
                    if (!f.isAfter(LocalDate.now().plusDays(1)) && f.getYear() >= 2000) {
                        return f;
                    }
                } catch (DateTimeException | NumberFormatException e) {
                    // «31/02» o un teléfono que parecía una fecha: a la siguiente.
                }
            }
        }
        return null;
    }

    /** La primera línea de la cabecera, para enseñar «MERCADONA, S.A.» o el nombre de la tienda. */
    private static String cabecera(List<String> lineas, Cadena cadena) {
        return lineas.isEmpty() ? null : recortar(lineas.getFirst(), 80);
    }

    /** Mayúsculas, sin dobles espacios y con los errores típicos del OCR en las cifras. */
    private static String limpiar(String linea) {
        String l = linea.toUpperCase(Locale.ROOT)
                .replace(' ', ' ')
                .replaceAll("[|]", " ")
                .replaceAll("\\s+", " ")
                .strip();
        // «1,9O» o «O,95»: una O entre cifras es un cero.
        return l.replaceAll("(?<=\\d[.,]?)O|O(?=[.,]?\\d)", "0");
    }

    private static boolean conLetras(String texto) {
        return texto.chars().filter(Character::isLetter).count() >= 2;
    }

    private static String texto(String descripcion) {
        return recortar(descripcion.replaceAll("\\s+[A-E]$", "").strip(), 120);
    }

    private static BigDecimal importe(String texto) {
        return new BigDecimal(texto.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cantidad(String texto, String unidad) {
        BigDecimal valor = new BigDecimal(texto.replace(',', '.'));
        return "G".equals(unidad) ? valor.divide(new BigDecimal(1000), 3, RoundingMode.HALF_UP) : valor;
    }

    private static String euros(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
    }

    private static String recortar(String texto, int largo) {
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }

    private record Pendiente(String texto) {
    }
}
