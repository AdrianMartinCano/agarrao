import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { DatosProducto, Producto } from './producto.model';

/**
 * Qué hay detrás de un código de barras. El back consulta Open Food Facts y lo guarda;
 * aquí no se habla con OFF directamente (pide identificarse y limita las consultas).
 */
@Injectable({ providedIn: 'root' })
export class ProductosService {
  private readonly http = inject(HttpClient);

  /** 404 = nadie lo conoce; 503 = OFF no responde ahora. */
  buscar(ean: string): Observable<Producto> {
    return this.http.get<RespuestaApi<Producto>>(`/api/productos/${ean}`).pipe(datos());
  }

  /** Ponerle nombre a uno que OFF no conoce: vale ya para todos los hogares. */
  nombrar(ean: string, producto: DatosProducto): Observable<Producto> {
    return this.http.put<RespuestaApi<Producto>>(`/api/productos/${ean}`, producto).pipe(datos());
  }
}

/** «Leche semidesnatada Hacendado»: lo que se apunta en la lista (máx. 80, como el back). */
export function nombreParaLista(producto: Pick<Producto, 'nombre' | 'marca'>): string {
  const marca = producto.marca && !producto.nombre.toLowerCase().includes(producto.marca.toLowerCase())
    ? ` ${producto.marca}`
    : '';
  return `${producto.nombre}${marca}`.slice(0, 80).trim();
}
