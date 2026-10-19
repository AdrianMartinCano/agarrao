package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Textos;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cuánto trae un envase, en kilos, litros o piezas: «6X1L» son 6 L, «500 g» 0,5 kg,
 * «12 uds» 12 piezas. Se lee de la cantidad de Open Food Facts o del texto del ticket.
 */
public record Medida(BigDecimal valor, Unidad unidad) {

    private static final String NUMERO = "(\\d+(?:\\.\\d+)?)";
    private static final String MEDIDAS = "(KG|KGS|KILOS?|GR?|GRS|GRAMOS|L|LT|LTS|LITROS?|ML|CL)";
    /** «6 x 1 L», «4X125G». */
    private static final Pattern PACK = Pattern.compile("\\b(\\d+)\\s*[X*]\\s*" + NUMERO + "\\s*" + MEDIDAS + "\\b");
    /** «1 L», «500G», «1.5 LITROS». */
    private static final Pattern SUELTA = Pattern.compile("(?<![\\d.])" + NUMERO + "\\s*" + MEDIDAS + "\\b");
    /** «12 UDS», «6 rollos», «30 lavados». */
    private static final Pattern PIEZAS = Pattern.compile("\\b(\\d+)\\s*(UDS?|UNIDADES|U|ROLLOS|PIEZAS|LAVADOS)\\b");

    public static Optional<Medida> leer(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        // «1'5 litros» y «1,5 L» son 1.5; «MEDIA DOCENA» antes que «DOCENA».
        String t = Textos.sinAcentos(texto).toUpperCase(Locale.ROOT).replaceAll("(\\d)['’,](\\d)", "$1.$2");
        Matcher m = PACK.matcher(t);
        if (m.find()) {
            return Optional.of(medida(new BigDecimal(m.group(2)), m.group(3))
                    .multiplicar(new BigDecimal(m.group(1))));
        }
        m = SUELTA.matcher(t);
        if (m.find()) {
            return Optional.of(medida(new BigDecimal(m.group(1)), m.group(2)));
        }
        m = PIEZAS.matcher(t);
        if (m.find()) {
            return Optional.of(new Medida(new BigDecimal(m.group(1)), Unidad.UD));
        }
        if (t.contains("MEDIA DOCENA")) {
            return Optional.of(new Medida(BigDecimal.valueOf(6), Unidad.UD));
        }
        if (t.matches(".*\\bDOCENA\\b.*")) {
            return Optional.of(new Medida(BigDecimal.valueOf(12), Unidad.UD));
        }
        return Optional.empty();
    }

    /** El tamaño en la unidad del genérico; vacío si no se pueden comparar (kilos frente a litros). */
    public Optional<BigDecimal> en(Unidad destino) {
        return unidad == destino && valor.signum() > 0 ? Optional.of(valor) : Optional.empty();
    }

    private Medida multiplicar(BigDecimal veces) {
        return new Medida(valor.multiply(veces), unidad);
    }

    private static Medida medida(BigDecimal numero, String medida) {
        return switch (medida) {
            case "G", "GR", "GRS", "GRAMOS" -> new Medida(numero.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP), Unidad.KG);
            case "ML" -> new Medida(numero.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP), Unidad.L);
            case "CL" -> new Medida(numero.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP), Unidad.L);
            case "L", "LT", "LTS", "LITRO", "LITROS" -> new Medida(numero, Unidad.L);
            default -> new Medida(numero, Unidad.KG);
        };
    }
}
