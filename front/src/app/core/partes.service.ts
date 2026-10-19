import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '@adrianmartincano/ng-auth';
import { Observable, tap } from 'rxjs';

import { RespuestaApi, datos } from './api';

/** Las partes de la app que cada persona enciende o apaga. */
export type Parte = 'LISTA' | 'PRECIOS' | 'GASTOS' | 'GASOLINA';

export const PARTES: readonly Parte[] = ['LISTA', 'PRECIOS', 'GASTOS', 'GASOLINA'];

/** Copia local para pintar la barra al momento, antes de que conteste el back. */
const CLAVE = 'partes';

/**
 * Qué partes usa la persona. Mientras llega la respuesta se usa la última conocida (o
 * todas): la barra no parpadea al abrir la app.
 */
@Injectable({ providedIn: 'root' })
export class PartesService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly _partes = signal<readonly Parte[]>(leerCopia());
  readonly partes = this._partes.asReadonly();
  private cargadoPara: string | null = null;

  readonly lista = computed(() => this._partes().includes('LISTA'));
  readonly precios = computed(() => this._partes().includes('PRECIOS'));
  readonly gastos = computed(() => this._partes().includes('GASTOS'));

  /** Una vez por persona y visita. */
  cargar(): void {
    const yo = this.auth.currentUser()?.id ?? null;
    if (!yo || this.cargadoPara === yo) return;
    this.cargadoPara = yo;
    this.http
      .get<RespuestaApi<Parte[]>>('/api/yo/partes')
      .pipe(datos())
      .subscribe({ next: (p) => this.poner(p), error: () => (this.cargadoPara = null) });
  }

  elegir(partes: readonly Parte[]): Observable<Parte[]> {
    return this.http
      .put<RespuestaApi<Parte[]>>('/api/yo/partes', { partes })
      .pipe(
        datos(),
        tap((p) => this.poner(p)),
      );
  }

  private poner(partes: readonly Parte[]): void {
    const ordenadas = PARTES.filter((p) => partes.includes(p));
    this._partes.set(ordenadas.length > 0 ? ordenadas : PARTES);
    try {
      localStorage.setItem(CLAVE, ordenadas.join(','));
    } catch {
      // Sin almacenamiento, vale para esta visita.
    }
  }
}

function leerCopia(): readonly Parte[] {
  try {
    const guardadas = (localStorage.getItem(CLAVE) ?? '').split(',').filter((p): p is Parte =>
      (PARTES as readonly string[]).includes(p),
    );
    return guardadas.length > 0 ? guardadas : PARTES;
  } catch {
    return PARTES;
  }
}
