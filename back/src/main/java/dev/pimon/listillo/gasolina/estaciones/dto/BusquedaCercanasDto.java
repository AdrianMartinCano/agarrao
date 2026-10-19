package dev.pimon.listillo.gasolina.estaciones.dto;

/**
 * Lo que devuelve una búsqueda: la página de estaciones y cómo está el precio por ahí.
 *
 * Van juntos y no mezclados a propósito. {@link PaginaDto} es genérico y se puede
 * reutilizar para cualquier lista; meterle campos de precios lo ataría a las gasolineras
 * y dejaría de servir para lo siguiente que haya que paginar.
 *
 * @param resumen  puede ser null: sin carburante elegido no hay un precio que promediar
 *                 —cada estación vende varios y a precios distintos— y una media de todos
 *                 mezclados no significaría nada
 */
public record BusquedaCercanasDto(
        PaginaDto<EstacionCercanaDto> estaciones,
        ResumenPreciosDto resumen
) {
}
