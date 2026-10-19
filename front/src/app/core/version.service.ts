import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { SwUpdate } from '@angular/service-worker';
import { Observable, map, tap } from 'rxjs';

import { VERSION_APP } from './version-app';

export type EstadoVersion = 'AL_DIA' | 'OPCIONAL' | 'OBLIGATORIA';

export interface ComprobacionVersion {
  estado: EstadoVersion;
  actual: string;
  ultima: string;
  minima: string;
  notas: string | null;
}

interface RespuestaApi<T> {
  data: T;
}

/**
 * ¿Está al día la app que tiene cargada este navegador?
 *
 * Quien decide es el back (`GET /api/app/version?actual=`): aquí solo se pregunta y se
 * guarda la respuesta en una señal que leen los avisos. Si la pregunta falla —sin red, el
 * back caído— no se bloquea nada: es una PWA y tiene que seguir abriendo en el súper sin
 * cobertura.
 */
@Injectable({ providedIn: 'root' })
export class VersionService {
  private readonly http = inject(HttpClient);
  private readonly sw = inject(SwUpdate);

  private readonly _comprobacion = signal<ComprobacionVersion | null>(null);
  readonly comprobacion = this._comprobacion.asReadonly();

  /** La versión que lleva ESTA build. Se enseña en pantalla para poder preguntarla. */
  readonly actual = VERSION_APP;

  comprobar(): Observable<ComprobacionVersion> {
    return this.http
      .get<RespuestaApi<ComprobacionVersion>>('/api/app/version', { params: { actual: this.actual } })
      .pipe(
        map((respuesta) => respuesta.data),
        tap((comprobacion) => this._comprobacion.set(comprobacion)),
      );
  }

  /** Cierra el aviso opcional hasta la próxima vez que se abra la app. */
  descartar(): void {
    const actual = this._comprobacion();
    if (actual?.estado === 'OPCIONAL') {
      this._comprobacion.set({ ...actual, estado: 'AL_DIA' });
    }
  }

  /**
   * Trae la versión nueva y se queda con ella.
   *
   * En una PWA instalada no hay botón de recargar, y aunque lo hubiera, el service worker
   * seguiría sirviendo lo que ya tenía. Por eso el orden: que MIRE si hay algo nuevo, que
   * lo ACTIVE y entonces recargar. Si algo falla —o no hay service worker, como en
   * desarrollo— se recarga igual: peor que no hacer nada no puede quedar.
   */
  async actualizar(): Promise<void> {
    try {
      if (this.sw.isEnabled) {
        await this.sw.checkForUpdate();
        await this.sw.activateUpdate();
      }
    } catch {
      // La recarga de abajo es la salida en cualquier caso.
    }
    location.reload();
  }
}
