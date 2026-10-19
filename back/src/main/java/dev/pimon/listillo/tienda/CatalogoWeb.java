package dev.pimon.listillo.tienda;

import dev.pimon.listillo.generico.Categoria;
import dev.pimon.listillo.ticket.Cadena;

import java.util.List;
import java.util.Set;

/** El catálogo web de una cadena. Una implementación por cadena. */
public interface CatalogoWeb {

    Cadena cadena();

    /** Todo el catálogo con precio. Lento (cientos de peticiones): solo desde la importación. */
    List<ProductoWeb> leer();

    /** A qué pasillos de Listillo corresponde una sección de la web (vacío = ninguno). */
    Set<Categoria> pasillos(String seccion);
}
