package dev.pimon.listillo.producto;

public enum OrigenProducto {
    /** Datos de Open Food Facts (licencia ODbL: hay que citar la fuente). */
    OPEN_FOOD_FACTS,
    /** OFF no lo conocía y alguien de Listillo le puso nombre. */
    USUARIO,
    /** OFF no lo conoce y nadie lo ha nombrado: se recuerda para no volver a preguntar ya. */
    DESCONOCIDO
}
