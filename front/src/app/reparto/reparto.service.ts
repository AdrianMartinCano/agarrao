import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { Categoria, FuentePrecio } from '../precios/generico.model';
import { Cadena, Unidad } from '../tickets/ticket.model';

export interface ArticuloRepartido {
  articuloId: string;
  nombre: string;
  cantidad: number;
  genericoId: string;
  generico: string;
  categoria: Categoria;
  retrato: string | null;
  /** El producto más barato de esa cadena. */
  producto: string;
  precioUnidad: number;
  unidad: Unidad;
  /** Su envase por la cantidad apuntada. */
  importe: number;
  fuente: FuentePrecio;
}

export interface Reparto {
  grupos: { cadena: Cadena; total: number; articulos: ArticuloRepartido[] }[];
  sinPrecio: { articuloId: string; nombre: string; generico: string | null }[];
  /** Comprando cada cosa donde sale más barata. */
  total: number;
  /** Yendo solo a una tienda (faltan = cosas sin precio allí). */
  enUna: { cadena: Cadena; total: number; faltan: number }[];
}

/** La lista pendiente repartida por el súper donde sale más barata cada cosa. */
@Injectable({ providedIn: 'root' })
export class RepartoService {
  private readonly http = inject(HttpClient);

  repartir(hogarId: string): Observable<Reparto> {
    return this.http.get<RespuestaApi<Reparto>>(`/api/hogares/${hogarId}/lista/reparto`).pipe(datos());
  }
}
