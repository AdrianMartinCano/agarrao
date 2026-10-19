import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Coordenadas } from './api.model';

/** Centro de Madrid: a dónde se cae la búsqueda si no hay ubicación del navegador. */
export const POSICION_POR_DEFECTO: Coordenadas = { latitud: 40.4168, longitud: -3.7038 };

/**
 * Cuánto se espera a que el navegador diga dónde estamos.
 *
 * ERA DE 10 SEGUNDOS Y SE QUEDABA CORTO: con `enableHighAccuracy: false` la posición sale
 * de wifi y antenas, que en un móvil con mala cobertura y sin nada en caché puede tardar
 * más que eso la primera vez. Pasado el plazo se cae al centro de Madrid, y quien lo sufre
 * es justo el que entra por primera vez.
 *
 * SUBIRLO NO CUESTA NADA porque la búsqueda es reactiva: mientras se resuelve ya se ven
 * resultados del sitio por defecto y, cuando llega la posición, se rehace sola. Lo único
 * que se retrasa es el aviso de error cuando de verdad no hay ubicación, y ese es el caso
 * raro. Más de medio minuto ya no es esperar a la señal: es esperar a nada.
 */
const TIEMPO_MAXIMO_MS = 30_000;

/**
 * Ubicación del usuario a través del navegador.
 *
 * Cuatro cosas que dan guerra y conviene tener presentes:
 *   - El navegador BLOQUEA la geolocalización fuera de HTTPS (localhost es la excepción).
 *     En producción sin certificado, esto no funciona y no hay forma de arreglarlo desde
 *     el código.
 *   - LOS NAVEGADORES INTEGRADOS DE WHATSAPP E INSTAGRAM la traen capada. Alguien que
 *     abra el enlace desde un chat puede no llegar a ver el diálogo, o verlo, aceptar y
 *     fallar igualmente, porque el permiso que falta es el de la app anfitriona. No se
 *     arregla desde aquí: hay que abrir la web en el navegador de verdad.
 *   - El usuario puede denegar el permiso, y entonces no se le puede volver a preguntar
 *     desde JavaScript: tiene que cambiarlo él en el navegador.
 *   - Puede no contestar nunca al diálogo, así que sin `timeout` la promesa se quedaría
 *     colgada y la aplicación esperando para siempre.
 */
@Injectable({ providedIn: 'root' })
export class GeolocalizacionService {
  posicionActual(): Observable<Coordenadas> {
    return new Observable<Coordenadas>((observador) => {
      if (!navigator.geolocation) {
        observador.error(new Error('Este navegador no puede darnos tu ubicación'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (posicion) => {
          observador.next({
            latitud: posicion.coords.latitude,
            longitud: posicion.coords.longitude,
          });
          observador.complete();
        },
        (error) => observador.error(new Error(mensajeDeError(error))),
        { enableHighAccuracy: false, timeout: TIEMPO_MAXIMO_MS, maximumAge: 300_000 },
      );
    });
  }
}

/**
 * Traduce el error del navegador a algo que un usuario entienda.
 *
 * `enableHighAccuracy: false` es deliberado: para elegir gasolinera sobra con la
 * precisión de la red, y el GPS tarda mucho más y gasta batería.
 */
function mensajeDeError(error: GeolocationPositionError): string {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return 'No nos has dado permiso para saber dónde estás. Puedes buscar igualmente en el centro de Madrid o cambiar el permiso en el navegador.';
    case error.POSITION_UNAVAILABLE:
      return 'No hemos podido averiguar dónde estás. Prueba otra vez en un momento.';
    case error.TIMEOUT:
      return 'Se ha tardado demasiado en localizarte.';
    default:
      return 'No hemos podido obtener tu ubicación.';
  }
}
