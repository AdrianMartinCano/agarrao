import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { Coordenadas, EstacionCercana, RespuestaApi } from './api.model';

/**
 * Favoritos del usuario.
 *
 * Mantiene en un signal el conjunto de ids marcados para que la lista de resultados
 * pueda pintar la estrella rellena sin preguntar al servidor por cada estación.
 */
@Injectable({ providedIn: 'root' })
export class FavoritosService {
  private readonly http = inject(HttpClient);

  private readonly ids = signal<ReadonlySet<string>>(new Set());

  /** Ids marcados como favoritos, para pintar el estado del botón. */
  readonly idsFavoritos = computed(() => this.ids());

  esFavorita(estacionId: string): boolean {
    return this.ids().has(estacionId);
  }

  listar(posicion: Coordenadas | null, carburante: string | null): Observable<EstacionCercana[]> {
    let params = new HttpParams();
    if (posicion) {
      params = params.set('latitud', posicion.latitud).set('longitud', posicion.longitud);
    }
    if (carburante) {
      params = params.set('carburante', carburante);
    }

    return this.http.get<RespuestaApi<EstacionCercana[]>>('/api/favoritos', { params }).pipe(
      map((respuesta) => respuesta.data),
      tap((favoritos) => this.ids.set(new Set(favoritos.map((f) => f.id)))),
    );
  }

  anadir(estacionId: string): Observable<void> {
    return this.http.post<RespuestaApi<void>>(`/api/favoritos/${estacionId}`, {}).pipe(
      map(() => undefined),
      tap(() => this.ids.update((actuales) => new Set(actuales).add(estacionId))),
    );
  }

  quitar(estacionId: string): Observable<void> {
    return this.http.delete<RespuestaApi<void>>(`/api/favoritos/${estacionId}`).pipe(
      map(() => undefined),
      tap(() =>
        this.ids.update((actuales) => {
          const copia = new Set(actuales);
          copia.delete(estacionId);
          return copia;
        }),
      ),
    );
  }

  /** Al cerrar sesión hay que olvidarlos, o el siguiente usuario vería estrellas ajenas. */
  limpiar(): void {
    this.ids.set(new Set());
  }
}
