import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { Categoria } from '../precios/generico.model';
import { Cadena } from '../tickets/ticket.model';

export interface GastoMes {
  /** «2026-10». */
  mes: string;
  total: number;
  tickets: number;
}

/** En qué se va el dinero; los tickets del súper son SUPER. */
export type TipoGasto = 'SUPER' | 'RESTAURANTE' | 'GASOLINA' | 'SUSCRIPCION' | 'OCIO' | 'CASA' | 'OTROS';

export const TIPOS_GASTO: readonly TipoGasto[] = [
  'SUPER',
  'RESTAURANTE',
  'GASOLINA',
  'SUSCRIPCION',
  'OCIO',
  'CASA',
  'OTROS',
];

export const NOMBRE_TIPO: Record<TipoGasto, string> = {
  SUPER: 'Súper',
  RESTAURANTE: 'Restaurantes',
  GASOLINA: 'Gasolina',
  SUSCRIPCION: 'Suscripciones',
  OCIO: 'Ocio',
  CASA: 'Casa',
  OTROS: 'Otros',
};

/** Un tono por tipo, de la misma paleta que los pasillos y sin dos parecidos. */
export const COLOR_TIPO: Record<TipoGasto, string> = {
  SUPER: '#7fd6a0',
  RESTAURANTE: '#ff9f6e',
  GASOLINA: '#6fb8f0',
  SUSCRIPCION: '#b98ce6',
  OCIO: '#ff8fab',
  CASA: '#f4c84f',
  OTROS: '#c9c2b8',
};

/** Un gasto anotado (no un ticket del súper). */
export interface GastoAnotado {
  id: string;
  concepto: string;
  importe: number;
  /** «2026-10-08». */
  fecha: string;
  tipo: TipoGasto;
  /** El gasto fijo del que salió (Netflix de octubre), si salió de uno. */
  fijoId: string | null;
}

export interface GastoAAnotar {
  concepto: string;
  importe: number;
  fecha: string;
  tipo: TipoGasto;
  /** Que se apunte solo cada mes, ese mismo día (hasta el 28). */
  cadaMes: boolean;
}

/** Lo que se apunta solo cada mes. */
export interface GastoFijo {
  id: string;
  concepto: string;
  importe: number;
  tipo: TipoGasto;
  dia: number;
}

export interface Gasto {
  mes: string;
  /** Tickets del súper más lo anotado. */
  total: number;
  tickets: number;
  /** Gastos anotados del mes. */
  anotados: number;
  /** De mayor a menor; los tickets cuentan como SUPER. */
  porTipo: { tipo: TipoGasto; total: number; veces: number }[];
  /** Lo que restaron los descuentos, en positivo. */
  descuentos: number;
  /** El mes más caro de los últimos doce hasta hoy: la vara fija del tarro. */
  referencia: number;
  /** Seis meses, del más viejo al más nuevo: hasta el de hoy si el elegido cabe. */
  meses: GastoMes[];
  porTienda: { cadena: Cadena; total: number; tickets: number }[];
  /** Categoría null: las líneas sin genérico. */
  porCategoria: { categoria: Categoria | null; total: number }[];
  top: { id: string; nombre: string; categoria: Categoria; retrato: string | null; total: number; compras: number }[];
}

/** El gasto del hogar por mes (tickets y anotados) y los gastos anotados. */
@Injectable({ providedIn: 'root' })
export class GastoService {
  private readonly http = inject(HttpClient);

  /** Sin mes, el actual. */
  resumen(hogarId: string, mes?: string): Observable<Gasto> {
    const params = mes ? new HttpParams().set('mes', mes) : undefined;
    return this.http.get<RespuestaApi<Gasto>>(`/api/hogares/${hogarId}/gasto`, { params }).pipe(datos());
  }

  /** Los anotados de un mes (sin mes, el actual), del más nuevo al más viejo. */
  anotados(hogarId: string, mes?: string): Observable<GastoAnotado[]> {
    const params = mes ? new HttpParams().set('mes', mes) : undefined;
    return this.http.get<RespuestaApi<GastoAnotado[]>>(this.url(hogarId), { params }).pipe(datos());
  }

  uno(hogarId: string, id: string): Observable<GastoAnotado> {
    return this.http.get<RespuestaApi<GastoAnotado>>(`${this.url(hogarId)}/${id}`).pipe(datos());
  }

  anotar(hogarId: string, gasto: GastoAAnotar): Observable<GastoAnotado> {
    return this.http.post<RespuestaApi<GastoAnotado>>(this.url(hogarId), gasto).pipe(datos());
  }

  cambiar(hogarId: string, id: string, gasto: GastoAAnotar): Observable<GastoAnotado> {
    return this.http.put<RespuestaApi<GastoAnotado>>(`${this.url(hogarId)}/${id}`, gasto).pipe(datos());
  }

  borrar(hogarId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.url(hogarId)}/${id}`);
  }

  fijos(hogarId: string): Observable<GastoFijo[]> {
    return this.http.get<RespuestaApi<GastoFijo[]>>(`${this.url(hogarId)}/fijos`).pipe(datos());
  }

  quitarFijo(hogarId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.url(hogarId)}/fijos/${id}`);
  }

  private url(hogarId: string): string {
    return `/api/hogares/${hogarId}/gastos`;
  }
}
