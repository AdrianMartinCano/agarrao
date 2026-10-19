package dev.pimon.listillo.gasolina.estaciones.dto;

/**
 * Un carburante para el desplegable del front.
 *
 * @param clave     la que se manda en las búsquedas (GASOLINA_95_E5)
 * @param etiqueta  cómo se le enseña al usuario ("Gasolina 95 E5")
 */
public record CarburanteDto(String clave, String etiqueta) {
}
