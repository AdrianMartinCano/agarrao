package dev.pimon.listillo.generico;

/**
 * De dónde sale un precio: un ticket del hogar, el catálogo web de la cadena (de hoy) u
 * Open Prices (lo subió alguien desde un ticket o una etiqueta, con su fecha; licencia ODbL).
 */
public enum FuentePrecio {
    TICKET,
    WEB,
    OPEN_PRICES
}
