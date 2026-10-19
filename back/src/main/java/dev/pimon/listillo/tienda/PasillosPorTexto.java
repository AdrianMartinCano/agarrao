package dev.pimon.listillo.tienda;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Textos;

import java.util.EnumSet;
import java.util.Set;

/**
 * Los pasillos de Listillo donde buscar el genérico de un producto, sacados de las palabras
 * de su sección en la web de la cadena («Pescado congelado», «Aperitivos > Patatas fritas y
 * snacks»). Para las cadenas cuyas secciones no son un árbol fijo (Aldi, Consum).
 *
 * Lo congelado manda: «Tortillas, croquetas y patatas» o «Pescado congelado» solo buscan en
 * Congelados, para que unas patatas para horno no pasen por patatas de bolsa.
 */
final class PasillosPorTexto {

    /** Sin pistas, en todos los de comida. */
    static final Set<Categoria> COMIDA = EnumSet.complementOf(EnumSet.of(Categoria.LIMPIEZA, Categoria.HIGIENE,
            Categoria.HOGAR, Categoria.OTROS));

    /** Bazar sin más pistas: hogar y limpieza (una fregona, un cubo). */
    static final Set<Categoria> BAZAR = EnumSet.of(Categoria.HOGAR, Categoria.LIMPIEZA);

    private PasillosPorTexto() {
    }

    /** Para secciones de bazar (Lidl, Aldi): con sus palabras o, si no dicen nada, hogar y limpieza. */
    static Set<Categoria> deBazar(String seccion) {
        Set<Categoria> pasillos = de(seccion);
        return pasillos == COMIDA ? BAZAR : pasillos;
    }

    static Set<Categoria> de(String seccion) {
        if (seccion == null || seccion.isBlank()) {
            return COMIDA;
        }
        String s = Textos.normalizar(seccion);
        if (s.matches(".*\\b(congelad|croqueta|tortilla|rebozad|helado|pizza).*")) {
            return EnumSet.of(Categoria.CONGELADOS);
        }
        Set<Categoria> pasillos = EnumSet.noneOf(Categoria.class);
        if (s.matches(".*\\b(fruta|verdura|hortaliza).*")) pasillos.addAll(EnumSet.of(Categoria.FRUTA, Categoria.VERDURA));
        if (s.matches(".*\\b(carne|charcuter|embutido|fiambre|pollo|cerdo|vacuno).*")) pasillos.add(Categoria.CARNE);
        if (s.matches(".*\\b(pescado|marisco).*")) pasillos.add(Categoria.PESCADO);
        if (s.matches(".*\\b(lacteo|leche|queso|huevo|yogur|postre|mantequilla|nata).*")) pasillos.add(Categoria.LACTEOS);
        if (s.matches(".*\\b(pan|panaderia|bolleria|tostada).*")) pasillos.addAll(EnumSet.of(Categoria.PANADERIA, Categoria.DESAYUNO));
        if (s.matches(".*\\b(chocolate|dulce|galleta|desayuno|cafe|cacao|cereal|infusion|mermelada).*")) pasillos.add(Categoria.DESAYUNO);
        if (s.matches(".*\\b(bebida|vino|cerveza|agua|zumo|refresco).*")) pasillos.add(Categoria.BEBIDAS);
        if (s.matches(".*\\b(limpieza|hogar|drogueria|detergente|lavavajillas|papel).*")) pasillos.add(Categoria.LIMPIEZA);
        if (s.matches(".*\\b(hogar|bazar|menaje|cocina|utensilio|ferreteria|bricolaje|iluminacion|electricidad|pila|fregona|cubo).*")) {
            pasillos.add(Categoria.HOGAR);
        }
        if (s.matches(".*\\b(higiene|cuidado|belleza|drogueria|papel|bucal|ducha|champu|desodorante).*")) pasillos.add(Categoria.HIGIENE);
        if (s.matches(".*\\b(aceite|conserva|pasta|arroz|legumbre|salsa|aperitivo|snack|patatas fritas|despensa|plato|gourmet|especia|azucar|harina|tomate|atun).*")) {
            pasillos.add(Categoria.DESPENSA);
        }
        if (s.matches(".*\\b(conserva|atun).*")) pasillos.add(Categoria.PESCADO);
        return pasillos.isEmpty() ? COMIDA : pasillos;
    }
}
