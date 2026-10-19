import { Coordenadas, EstacionCercana } from './api.model';

/**
 * Los dos carburantes que se enseñan siempre en el globo del mapa.
 *
 * Las etiquetas coinciden a propósito con las del desplegable (el backend las llama
 * igual, en Carburante.java): ver "Diésel" en el filtro y "Gasóleo A" en el globo haría
 * dudar de si son lo mismo.
 */
export const CARBURANTES_DESTACADOS = [
  { clave: 'GASOLINA_95_E5', etiqueta: 'Gasolina 95' },
  { clave: 'GASOLEO_A', etiqueta: 'Diésel' },
] as const;

/**
 * Enlace que abre Google Maps con la ruta hasta la gasolinera.
 *
 * Se manda la COORDENADA y no el texto de la dirección a propósito. Las direcciones del
 * Ministerio vienen abreviadas y sucias ("CL PASEO ACACIAS, 8", "AUTOVIA NOROESTE KM.
 * 111"), y Google las interpreta mal o las lleva al sitio equivocado. La latitud y la
 * longitud no admiten interpretación.
 *
 * El nombre va en `destination` solo como etiqueta visible: al llevar `destination` con
 * coordenadas, el punto exacto ya está fijado.
 *
 * Funciona igual en el móvil (abre la app de Google Maps si está instalada) y en el
 * escritorio (abre la web).
 */
export function urlComoLlegar(estacion: EstacionCercana): string {
  const destino = `${estacion.latitud},${estacion.longitud}`;
  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destino)}`;
}

/** Precio de un carburante concreto, o null si esa estación no lo vende. */
export function precioDe(estacion: EstacionCercana, clave: string): number | null {
  return estacion.precios[clave] ?? null;
}

/**
 * Viajes: Google Maps con el viaje entero y la gasolinera como parada. Igual que arriba,
 * todo por coordenadas.
 */
export function urlViajeConParada(origen: Coordenadas, destino: Coordenadas, parada: EstacionCercana): string {
  const punto = (latitud: number, longitud: number) => encodeURIComponent(`${latitud},${longitud}`);
  return (
    'https://www.google.com/maps/dir/?api=1&travelmode=driving' +
    `&origin=${punto(origen.latitud, origen.longitud)}` +
    `&destination=${punto(destino.latitud, destino.longitud)}` +
    `&waypoints=${punto(parada.latitud, parada.longitud)}`
  );
}
