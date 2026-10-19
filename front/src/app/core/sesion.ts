import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@adrianmartincano/ng-auth';
import { catchError, throwError } from 'rxjs';

/*
 * Guardas propias en vez de las de ng-auth: las de la librería mandan a /login (aquí no
 * existe) y la de invitados redirige a «/», que es la propia portada.
 */

/** Pantallas con sesión: sin ella, a la portada. */
export const conSesion: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() || inject(Router).createUrlTree(['/']);
};

/** La portada: con sesión ya abierta, directo a la lista. */
export const sinSesion: CanActivateFn = () => {
  const auth = inject(AuthService);
  return !auth.isLoggedIn() || inject(Router).createUrlTree(['/lista']);
};

/**
 * Sesión caducada o cuenta borrada: el back contesta 401 y la app vuelve a la portada en
 * vez de quedarse con pantallas que fallan sin decir por qué. No toca las de /api/auth
 * (un 401 al entrar es «contraseña mal», no «sesión caducada»).
 */
export const sesionCaducada: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !peticion.url.includes('/api/auth/')) {
        auth.logout();
        void router.navigateByUrl('/', { replaceUrl: true });
      }
      return throwError(() => error);
    }),
  );
};
