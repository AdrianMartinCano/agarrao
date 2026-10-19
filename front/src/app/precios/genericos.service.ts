import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { Unidad } from '../tickets/ticket.model';
import {
  Busqueda,
  EnCadaSuper,
  Categoria,
  ClasificacionLinea,
  Clasificar,
  Generico,
  GenericoDetalle,
  GenericoResumen,
} from './generico.model';

/**
 * Los genéricos del hogar (el catálogo de Listillo más los propios) y su comparación por
 * precio por unidad, sacada de los tickets del hogar.
 */
@Injectable({ providedIn: 'root' })
export class GenericosService {
  private readonly http = inject(HttpClient);

  catalogo(hogarId: string): Observable<GenericoResumen[]> {
    return this.http.get<RespuestaApi<GenericoResumen[]>>(this.url(hogarId)).pipe(datos());
  }

  detalle(hogarId: string, id: string): Observable<GenericoDetalle> {
    return this.http.get<RespuestaApi<GenericoDetalle>>(`${this.url(hogarId)}/${id}`).pipe(datos());
  }

  crear(
    hogarId: string,
    nombre: string,
    categoria: Categoria,
    unidad: Unidad,
  ): Observable<Generico> {
    return this.http
      .post<RespuestaApi<Generico>>(this.url(hogarId), { nombre, categoria, unidad })
      .pipe(datos());
  }

  borrar(hogarId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.url(hogarId)}/${id}`);
  }

  /** Corregir qué genérico es un producto de ticket (o que ninguno) y cuánto trae. */
  clasificar(hogarId: string, peticion: Clasificar): Observable<void> {
    return this.http.put<void>(`${this.url(hogarId)}/clasificacion`, peticion);
  }

  /** Un producto de la web que no es este genérico (el catálogo web es común a todos). */
  /** {@code quitarDe}: el genérico propio desde el que se dice «No es esto» (solo se quita de ese). */
  clasificarWeb(
    hogarId: string,
    cadena: string,
    idExterno: string,
    genericoId: string | null,
    quitarDe: string | null = null,
  ): Observable<void> {
    return this.http.put<void>(`${this.url(hogarId)}/clasificacion-web`, {
      cadena,
      idExterno,
      genericoId,
      quitarDe,
    });
  }

  /** Cualquier cosa en las webs de los súper (también el bazar): lo más barato de cada una. */
  /** Ese producto (de la web de un súper) en cada súper. */
  enCadaSuper(hogarId: string, cadena: string, idExterno: string): Observable<EnCadaSuper> {
    return this.http
      .get<RespuestaApi<EnCadaSuper>>(`/api/hogares/${hogarId}/precios/en-cada-super`, {
        params: { cadena, id: idExterno },
      })
      .pipe(datos());
  }

  buscar(hogarId: string, texto: string): Observable<Busqueda> {
    return this.http
      .get<RespuestaApi<Busqueda>>(`/api/hogares/${hogarId}/precios/buscar`, {
        params: { q: texto },
      })
      .pipe(datos());
  }

  deTicket(hogarId: string, ticketId: string): Observable<ClasificacionLinea[]> {
    return this.http
      .get<RespuestaApi<ClasificacionLinea[]>>(`${this.url(hogarId)}/ticket/${ticketId}`)
      .pipe(datos());
  }

  private url(hogarId: string): string {
    return `/api/hogares/${hogarId}/genericos`;
  }
}
