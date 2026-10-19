import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@adrianmartincano/ng-auth';

const CLAVE = 'listillo.invitacion';

/**
 * El enlace de invitación (`/unirse/ABCD2345`). Sin sesión, el código se guarda y se va
 * a la portada a entrar; con sesión, a «Empezar» con el código ya escrito. Nunca une
 * sin preguntar: la persona ve a qué hogar entra y lo confirma.
 */
export const enlaceDeInvitacion: CanActivateFn = (ruta) => {
  const router = inject(Router);
  const codigo = String(ruta.paramMap.get('codigo') ?? '');
  if (!inject(AuthService).isLoggedIn()) {
    guardarInvitacion(codigo);
    return router.createUrlTree(['/']);
  }
  return router.createUrlTree(['/empezar'], { queryParams: { codigo } });
};

export function guardarInvitacion(codigo: string): void {
  try {
    localStorage.setItem(CLAVE, codigo);
  } catch {
    // Sin almacenamiento la invitación se pierde: tocará escribir el código a mano.
  }
}

/** La invitación pendiente, una sola vez: después se borra. */
export function tomarInvitacion(): string | null {
  try {
    const codigo = localStorage.getItem(CLAVE);
    localStorage.removeItem(CLAVE);
    return codigo;
  } catch {
    return null;
  }
}
