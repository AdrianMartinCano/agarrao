import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { Hogar, HogarResumen, Invitacion } from './hogar.model';

const CLAVE_ACTIVO = 'listillo.hogar';

/**
 * Mis hogares y cuál tengo abierto. El abierto se recuerda en este navegador: quien está
 * en dos hogares vuelve al último que miró.
 */
@Injectable({ providedIn: 'root' })
export class HogaresService {
  private readonly http = inject(HttpClient);

  private readonly _hogares = signal<HogarResumen[] | null>(null);
  /** Null mientras no se han pedido. */
  readonly hogares = this._hogares.asReadonly();

  private readonly activoId = signal<string | null>(leerActivo());
  /** El abierto; si el recordado ya no está (me fui, me echaron), el primero. */
  readonly activo = computed((): HogarResumen | null => {
    const hogares = this._hogares() ?? [];
    return hogares.find((h) => h.id === this.activoId()) ?? hogares.at(0) ?? null;
  });

  cargar(): Observable<HogarResumen[]> {
    return this.http.get<RespuestaApi<HogarResumen[]>>('/api/hogares').pipe(
      datos(),
      tap((hogares) => this._hogares.set(hogares)),
    );
  }

  elegir(id: string): void {
    this.activoId.set(id);
    guardarActivo(id);
  }

  detalle(id: string): Observable<Hogar> {
    return this.http.get<RespuestaApi<Hogar>>(`/api/hogares/${id}`).pipe(datos());
  }

  /** A qué hogar lleva un código, sin entrar todavía. */
  invitacion(codigo: string): Observable<Invitacion> {
    return this.http
      .get<RespuestaApi<Invitacion>>(`/api/hogares/invitacion/${encodeURIComponent(codigo)}`)
      .pipe(datos());
  }

  /** Crear o unirse deja ese hogar abierto. */
  crear(nombre: string): Observable<Hogar> {
    return this.http
      .post<RespuestaApi<Hogar>>('/api/hogares', { nombre })
      .pipe(datos(), this.abrirYRecargar());
  }

  unirse(codigo: string): Observable<Hogar> {
    return this.http
      .post<RespuestaApi<Hogar>>('/api/hogares/unirse', { codigo })
      .pipe(datos(), this.abrirYRecargar());
  }

  renombrar(id: string, nombre: string): Observable<Hogar> {
    return this.http
      .patch<RespuestaApi<Hogar>>(`/api/hogares/${id}`, { nombre })
      .pipe(datos(), this.abrirYRecargar());
  }

  /** Qué supermercados ve y compara la casa. */
  elegirCadenas(id: string, cadenas: string[]): Observable<Hogar> {
    return this.http.put<RespuestaApi<Hogar>>(`/api/hogares/${id}/cadenas`, { cadenas }).pipe(datos());
  }

  nuevoCodigo(id: string): Observable<Hogar> {
    return this.http.post<RespuestaApi<Hogar>>(`/api/hogares/${id}/codigo`, {}).pipe(datos());
  }

  salir(id: string): Observable<HogarResumen[]> {
    return this.http.delete<void>(`/api/hogares/${id}/miembros/yo`).pipe(switchMap(() => this.cargar()));
  }

  echar(id: string, usuarioId: string): Observable<Hogar> {
    return this.http
      .delete<void>(`/api/hogares/${id}/miembros/${encodeURIComponent(usuarioId)}`)
      .pipe(switchMap(() => this.detalle(id)));
  }

  /** Al cerrar sesión: que la siguiente cuenta no herede los hogares de la anterior. */
  olvidar(): void {
    this._hogares.set(null);
  }

  private abrirYRecargar() {
    return (fuente: Observable<Hogar>) =>
      fuente.pipe(
        tap((hogar) => this.elegir(hogar.id)),
        switchMap((hogar) => this.cargar().pipe(switchMap(() => [hogar]))),
      );
  }
}

function leerActivo(): string | null {
  try {
    return localStorage.getItem(CLAVE_ACTIVO);
  } catch {
    return null;
  }
}

function guardarActivo(id: string): void {
  try {
    localStorage.setItem(CLAVE_ACTIVO, id);
  } catch {
    // Navegación privada sin almacenamiento: se abrirá el primero, sin más.
  }
}
