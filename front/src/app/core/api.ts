import { HttpErrorResponse } from '@angular/common/http';
import { OperatorFunction, map } from 'rxjs';

/** El sobre de todas las respuestas del back (`ApiResponse` de spring-common). */
export interface RespuestaApi<T> {
  data: T;
  message?: string;
}

/** Saca `data` del sobre. */
export function datos<T>(): OperatorFunction<RespuestaApi<T>, T> {
  return map((respuesta) => respuesta.data);
}

/**
 * El mensaje que el back escribió para la persona («Ese código no es de ningún hogar…»),
 * o uno genérico si no hay (sin red, back caído).
 */
export function mensajeDeError(error: unknown, porDefecto = 'No se ha podido. Inténtalo otra vez.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Sin conexión. Prueba otra vez en un momento.';
    const cuerpo: unknown = error.error;
    if (cuerpo && typeof cuerpo === 'object' && 'message' in cuerpo && typeof cuerpo.message === 'string') {
      return cuerpo.message;
    }
  }
  return porDefecto;
}
