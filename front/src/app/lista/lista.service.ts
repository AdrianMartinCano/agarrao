import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '@adrianmartincano/ng-auth';
import { Observable, finalize, tap } from 'rxjs';

import { RespuestaApi, datos, mensajeDeError } from '../core/api';
import { Articulo } from './articulo.model';

/**
 * La lista del hogar abierto.
 *
 * Los cambios son optimistas: se ven al momento y luego llega la lista del back, que manda
 * (todas las respuestas traen la lista entera). Para que una respuesta vieja no pise un
 * cambio más nuevo, cada petición lleva su número y solo cuenta la última.
 */
@Injectable({ providedIn: 'root' })
export class ListaService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private hogarId: string | null = null;
  /** Número de la última petición lanzada: las respuestas anteriores se ignoran. */
  private ultima = 0;
  private readonly enVuelo = signal(0);

  private readonly _articulos = signal<Articulo[] | null>(null);
  /** Null hasta la primera carga. */
  readonly articulos = this._articulos.asReadonly();
  readonly error = signal<string | null>(null);
  readonly guardando = computed(() => this.enVuelo() > 0);

  readonly pendientes = computed(() => (this._articulos() ?? []).filter((a) => !a.comprado));
  /** Lo último echado al carro, arriba (el back ordena igual; esto cubre el cambio optimista). */
  readonly enElCarro = computed(() =>
    (this._articulos() ?? [])
      .filter((a) => a.comprado)
      .sort((a, b) => (b.compradoEn ?? '').localeCompare(a.compradoEn ?? '')),
  );

  /** Cambiar de hogar vacía la lista para no enseñar la del anterior ni un instante. */
  abrir(hogarId: string): void {
    if (this.hogarId !== hogarId) {
      this.hogarId = hogarId;
      this._articulos.set(null);
      this.error.set(null);
    }
    this.refrescar();
  }

  /**
   * Trae lo que hayan cambiado los demás. Si hay un cambio propio en camino no pregunta:
   * su respuesta ya traerá la lista al día.
   */
  refrescar(): void {
    if (!this.hogarId || this.enVuelo() > 0) return;
    this.enviar(this.http.get<RespuestaApi<Articulo[]>>(this.url()), false);
  }

  /** `ean`: si se apuntó escaneando (la fila queda enlazada al producto). */
  apuntar(nombre: string, cantidad = 1, ean?: string): void {
    const limpio = nombre.trim();
    if (!limpio) return;
    // Igual que el back: el mismo envase, o lo mismo escrito, aún sin comprar, suma.
    const igual =
      (ean ? this.pendientes().find((a) => a.ean === ean) : undefined) ??
      this.pendientes().find((a) => normalizar(a.nombre) === normalizar(limpio));
    if (igual) {
      this.cambiarLocal(igual.id, { cantidad: Math.min(99, igual.cantidad + cantidad) });
    } else {
      this._articulos.update((lista) => [
        ...(lista ?? []),
        {
          id: `nuevo-${Date.now()}`,
          nombre: limpio,
          cantidad,
          comprado: false,
          apuntadoPor: null,
          apuntadoPorId: this.auth.currentUser()?.id ?? '',
          ean: ean ?? null,
          creadoEn: new Date().toISOString(),
          compradoEn: null,
        },
      ]);
    }
    this.enviar(this.http.post<RespuestaApi<Articulo[]>>(this.url(), { nombre: limpio, cantidad, ean }));
  }

  marcar(articulo: Articulo, comprado: boolean): void {
    this.cambiarLocal(articulo.id, { comprado, compradoEn: comprado ? new Date().toISOString() : null });
    this.enviar(this.http.patch<RespuestaApi<Articulo[]>>(`${this.url()}/${articulo.id}`, { comprado }));
  }

  cambiarCantidad(articulo: Articulo, cantidad: number): void {
    const acotada = Math.max(1, Math.min(99, cantidad));
    if (acotada === articulo.cantidad) return;
    this.cambiarLocal(articulo.id, { cantidad: acotada });
    this.enviar(this.http.patch<RespuestaApi<Articulo[]>>(`${this.url()}/${articulo.id}`, { cantidad: acotada }));
  }

  quitar(articulo: Articulo): void {
    this._articulos.update((lista) => (lista ?? []).filter((a) => a.id !== articulo.id));
    this.enviar(this.http.delete<RespuestaApi<Articulo[]>>(`${this.url()}/${articulo.id}`));
  }

  vaciarCarro(): void {
    this._articulos.update((lista) => (lista ?? []).filter((a) => !a.comprado));
    this.enviar(this.http.delete<RespuestaApi<Articulo[]>>(`${this.url()}/comprados`));
  }

  private enviar(peticion: Observable<RespuestaApi<Articulo[]>>, esCambio = true): void {
    const numero = ++this.ultima;
    if (esCambio) this.enVuelo.update((n) => n + 1);
    peticion
      .pipe(
        datos(),
        tap({
          next: (lista) => {
            if (numero === this.ultima) {
              this._articulos.set(lista);
              this.error.set(null);
            }
          },
          error: (e: unknown) => {
            this.error.set(
              mensajeDeError(e, esCambio ? 'No se ha guardado. Revisa la conexión.' : 'No se ha podido cargar la lista.'),
            );
          },
        }),
        finalize(() => {
          if (esCambio) this.enVuelo.update((n) => n - 1);
          // Tras un cambio fallido, lo que hay en pantalla puede no ser verdad: se pide de nuevo.
          if (esCambio && this.error()) this.refrescar();
        }),
      )
      .subscribe({ error: () => undefined });
  }

  private cambiarLocal(id: string, cambio: Partial<Articulo>): void {
    this._articulos.update((lista) => (lista ?? []).map((a) => (a.id === id ? { ...a, ...cambio } : a)));
  }

  private url(): string {
    return `/api/hogares/${this.hogarId}/lista`;
  }
}

function normalizar(nombre: string): string {
  return nombre.trim().toLocaleLowerCase('es');
}
