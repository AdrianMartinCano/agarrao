import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RespuestaApi, datos } from '../core/api';
import { Borrador, Guardado, Ticket, TicketAGuardar, TicketResumen } from './ticket.model';

/**
 * Tickets del hogar. Leer no guarda: devuelve un borrador para repasar. El PDF se lee en el
 * back; la foto se lee en el móvil (ocr.ts) y aquí solo viaja su texto.
 */
@Injectable({ providedIn: 'root' })
export class TicketsService {
  private readonly http = inject(HttpClient);

  leerPdf(hogarId: string, archivo: File): Observable<Borrador> {
    const formulario = new FormData();
    formulario.append('archivo', archivo);
    return this.http.post<RespuestaApi<Borrador>>(`${this.url(hogarId)}/leer-pdf`, formulario).pipe(datos());
  }

  leerTexto(hogarId: string, texto: string): Observable<Borrador> {
    return this.http.post<RespuestaApi<Borrador>>(`${this.url(hogarId)}/leer-texto`, { texto }).pipe(datos());
  }

  guardar(hogarId: string, ticket: TicketAGuardar): Observable<Guardado> {
    return this.http.post<RespuestaApi<Guardado>>(this.url(hogarId), ticket).pipe(datos());
  }

  listar(hogarId: string): Observable<TicketResumen[]> {
    return this.http.get<RespuestaApi<TicketResumen[]>>(this.url(hogarId)).pipe(datos());
  }

  detalle(hogarId: string, id: string): Observable<Ticket> {
    return this.http.get<RespuestaApi<Ticket>>(`${this.url(hogarId)}/${id}`).pipe(datos());
  }

  borrar(hogarId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.url(hogarId)}/${id}`);
  }

  private url(hogarId: string): string {
    return `/api/hogares/${hogarId}/tickets`;
  }
}
