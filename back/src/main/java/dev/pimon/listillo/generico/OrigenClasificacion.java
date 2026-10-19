package dev.pimon.listillo.generico;

/** Quién decidió el genérico de un producto: Listillo adivinando o una persona. */
public enum OrigenClasificacion {
    /** Adivinado: se puede volver a adivinar si aparece algo mejor. */
    AUTO,
    /** Corregido a mano: no se toca nunca solo. */
    USUARIO
}
