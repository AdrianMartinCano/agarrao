package dev.pimon.listillo.producto;

/** Lo que devuelve un catálogo externo de un producto, ya limpio y recortado a las columnas. */
public record DatosProducto(String nombre, String marca, String cantidad, String imagenUrl, String categorias) {
}
