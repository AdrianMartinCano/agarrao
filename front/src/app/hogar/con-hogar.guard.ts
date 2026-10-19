import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';

import { HogaresService } from './hogares.service';

/**
 * Casi todo cuelga de un hogar. Quien entra sin ninguno NO pasa por «Empezar»: se le crea
 * uno («Mi casa», que puede renombrar e invitar a otros desde Hogar) y sigue a la app. Es
 * una app de gastos para cualquiera, no solo para casas compartidas.
 * Quien llega con un enlace de invitación va a «Empezar» desde la portada (y entra en esa
 * casa), así que aquí no se le crea otra. Si la carga falla (sin red), se deja pasar: la
 * pantalla enseñará su error; si falla crear, a «Empezar» como antes.
 */
export const conHogar: CanActivateFn = () => {
  const hogares = inject(HogaresService);
  const router = inject(Router);
  const crearSiFalta = (lista: unknown[]) =>
    lista.length > 0
      ? of(true)
      : hogares.crear(PRIMER_HOGAR).pipe(
          map(() => true),
          catchError(() => of(router.createUrlTree(['/empezar']))),
        );
  const cargados = hogares.hogares();
  if (cargados) {
    return crearSiFalta(cargados);
  }
  return hogares.cargar().pipe(
    switchMap((lista) => crearSiFalta(lista)),
    catchError(() => of(true)),
  );
};

/** El nombre del hogar que se crea solo al entrar por primera vez. */
export const PRIMER_HOGAR = 'Mi casa';
