import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';

import {
  BusquedaCercanas,
  Carburante,
  Coordenadas,
  EstadoCatalogo,
  FiltrosBusqueda,
  Lugar,
  RespuestaApi,
  Viaje,
} from './api.model';

/** Consulta pública de gasolineras. No necesita sesión. */
@Injectable({ providedIn: 'root' })
export class EstacionesService {
  private readonly http = inject(HttpClient);

  /**
   * La lista de carburantes no cambia nunca durante una visita, así que se pide una
   * sola vez y se comparte entre todos los que la necesiten (`shareReplay`). Sin esto,
   * cada componente que la usara dispararía su propia petición.
   */
  private readonly carburantes$ = this.http
    .get<RespuestaApi<Carburante[]>>('/api/estaciones/carburantes')
    .pipe(
      map((respuesta) => respuesta.data),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

  carburantes(): Observable<Carburante[]> {
    return this.carburantes$;
  }

  cercanas(
    posicion: Coordenadas,
    filtros: FiltrosBusqueda,
    pagina = 0,
    tamano = 20,
  ): Observable<BusquedaCercanas> {
    let params = new HttpParams()
      .set('latitud', posicion.latitud)
      .set('longitud', posicion.longitud)
      .set('radioKm', filtros.radioKm)
      .set('orden', filtros.orden)
      .set('pagina', pagina)
      .set('tamano', tamano);

    // Sin carburante el backend devuelve todas las estaciones; solo se manda si el
    // usuario ha elegido uno.
    if (filtros.carburante) {
      params = params.set('carburante', filtros.carburante);
    }

    return this.http
      .get<RespuestaApi<BusquedaCercanas>>('/api/estaciones/cercanas', { params })
      .pipe(map((respuesta) => respuesta.data));
  }

  /**
   * Municipios y códigos postales que encajan con lo escrito.
   *
   * Los resuelve el backend con los datos del propio Ministerio, sin geocodificador
   * externo, así que se puede llamar en cada tecla sin gastar cuota de nadie. Aun así el
   * componente espera a que se deje de escribir, por no hacer una petición por letra.
   */
  lugares(texto: string): Observable<Lugar[]> {
    return this.http
      .get<RespuestaApi<Lugar[]>>('/api/estaciones/lugares', {
        params: new HttpParams().set('q', texto),
      })
      .pipe(map((respuesta) => respuesta.data));
  }

  /** Viajes: gasolineras de camino entre dos puntos, de la más barata a la más cara. */
  ruta(origen: Coordenadas, destino: Coordenadas, carburante: string, desvioKm: number): Observable<Viaje> {
    const params = new HttpParams()
      .set('origenLatitud', origen.latitud)
      .set('origenLongitud', origen.longitud)
      .set('destinoLatitud', destino.latitud)
      .set('destinoLongitud', destino.longitud)
      .set('carburante', carburante)
      .set('desvioKm', desvioKm);
    return this.http
      .get<RespuestaApi<Viaje>>('/api/estaciones/ruta', { params })
      .pipe(map((respuesta) => respuesta.data));
  }

  estado(): Observable<EstadoCatalogo> {
    return this.http
      .get<RespuestaApi<EstadoCatalogo>>('/api/estaciones/estado')
      .pipe(map((respuesta) => respuesta.data));
  }
}
