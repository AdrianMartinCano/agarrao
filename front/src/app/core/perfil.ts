import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuthService } from '@adrianmartincano/ng-auth';
import { Observable, map, tap } from 'rxjs';

import { RespuestaApi } from './api';

/** Clave donde ng-auth guarda el usuario (`${tokenKey}_user`). */
const CLAVE_USUARIO = 'auth_token_user';

/**
 * Lo de la propia persona. Cambiar el nombre va contra `PUT /api/users/me` de spring-users;
 * ng-auth no sabe refrescar el usuario, así que se le actualiza a mano (señal y localStorage).
 *
 * Ojo: la respuesta del login (`LoginResponse` de spring-security) no trae el nombre y
 * ng-auth pone el email en su lugar. Por eso, al volver a entrar, salía el email aunque
 * el nombre bueno estuviera guardado. `recuperarNombre()` lo pide a `GET /api/users/me`.
 */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  cambiarNombre(nombre: string): Observable<string> {
    return this.http.put<RespuestaApi<{ name: string }>>('/api/users/me', { name: nombre }).pipe(
      map((r) => r.data?.name ?? nombre),
      tap((nuevo) => this.ponerNombre(nuevo)),
    );
  }

  /** Si el usuario guardado lleva el email por nombre (recién entrado), trae el de verdad. */
  recuperarNombre(): void {
    const yo = this.auth.currentUser();
    if (!yo || yo.name !== yo.email) return;
    this.http
      .get<RespuestaApi<{ name: string | null }>>('/api/users/me')
      .subscribe({ next: (r) => r.data?.name && this.ponerNombre(r.data.name), error: () => undefined });
  }

  private ponerNombre(nuevo: string): void {
    const yo = this.auth.currentUser();
    if (!yo || yo.name === nuevo) return;
    const actualizado = { ...yo, name: nuevo };
    this.auth.currentUser.set(actualizado);
    try {
      localStorage.setItem(CLAVE_USUARIO, JSON.stringify(actualizado));
    } catch {
      // Sin almacenamiento, el nombre nuevo vale para esta visita.
    }
  }
}
