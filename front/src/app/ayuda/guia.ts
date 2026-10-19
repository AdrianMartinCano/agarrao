import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

const CLAVE = 'guia-vista';

/**
 * Si la persona ya ha visto la guía en este dispositivo. Es una comodidad por dispositivo
 * (localStorage): en otro móvil la verá una vez más, y sin almacenamiento no se insiste
 * más de una vez por visita.
 */
@Injectable({ providedIn: 'root' })
export class GuiaService {
  readonly vista = signal(leer());

  marcarVista(): void {
    this.vista.set(true);
    try {
      localStorage.setItem(CLAVE, '1');
    } catch {
      // Sin almacenamiento, vale para esta visita.
    }
  }
}

function leer(): boolean {
  try {
    return localStorage.getItem(CLAVE) === '1';
  } catch {
    return false;
  }
}

/** La primera vez (en este dispositivo), antes de la pantalla que sea, la guía. */
export const guiaLaPrimeraVez: CanActivateFn = () =>
  inject(GuiaService).vista() || inject(Router).createUrlTree(['/guia']);
