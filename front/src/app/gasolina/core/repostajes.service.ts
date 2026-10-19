import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { HogaresService } from '../../hogar/hogares.service';
import { Observable, map } from 'rxjs';

import { RespuestaApi } from './api.model';
import {
  Estadisticas,
  NuevoRepostaje,
  NuevoVehiculo,
  Repostaje,
  Vehiculo,
} from './repostajes.model';

/** Vehículos y repostajes del usuario. Todo exige sesión. */
@Injectable({ providedIn: 'root' })
export class RepostajesService {
  private readonly http = inject(HttpClient);
  private readonly hogares = inject(HogaresService);

  /** En Agarrao los coches y repostajes son del hogar abierto. */
  private base(): string {
    return `/api/hogares/${this.hogares.activo()?.id ?? ''}`;
  }

  vehiculos(): Observable<Vehiculo[]> {
    return this.http
      .get<RespuestaApi<Vehiculo[]>>(`${this.base()}/vehiculos`)
      .pipe(map((respuesta) => respuesta.data));
  }

  crearVehiculo(vehiculo: NuevoVehiculo): Observable<Vehiculo> {
    return this.http
      .post<RespuestaApi<Vehiculo>>(`${this.base()}/vehiculos`, vehiculo)
      .pipe(map((respuesta) => respuesta.data));
  }

  /**
   * Corrige nombre, carburante o matrícula.
   *
   * Cambiar el carburante no reescribe los repostajes ya apuntados: cada uno guarda el
   * suyo. Solo cambia lo que se propone la próxima vez.
   */
  actualizarVehiculo(id: string, vehiculo: NuevoVehiculo): Observable<Vehiculo> {
    return this.http
      .put<RespuestaApi<Vehiculo>>(`${this.base()}/vehiculos/${id}`, vehiculo)
      .pipe(map((respuesta) => respuesta.data));
  }

  /** Lo archiva: sus repostajes se conservan. */
  archivarVehiculo(id: string): Observable<void> {
    return this.http.delete<RespuestaApi<void>>(`${this.base()}/vehiculos/${id}`).pipe(map(() => undefined));
  }

  estadisticas(vehiculoId: string): Observable<Estadisticas> {
    return this.http
      .get<RespuestaApi<Estadisticas>>(`${this.base()}/vehiculos/${vehiculoId}/estadisticas`)
      .pipe(map((respuesta) => respuesta.data));
  }

  repostajes(): Observable<Repostaje[]> {
    return this.http
      .get<RespuestaApi<Repostaje[]>>(`${this.base()}/repostajes`)
      .pipe(map((respuesta) => respuesta.data));
  }

  crearRepostaje(repostaje: NuevoRepostaje): Observable<Repostaje> {
    return this.http
      .post<RespuestaApi<Repostaje>>(`${this.base()}/repostajes`, repostaje)
      .pipe(map((respuesta) => respuesta.data));
  }

  /**
   * Corrige un repostaje.
   *
   * El precio medio de la provincia solo se rehace si cambia la gasolinera: es un dato
   * con fecha, y meterle el de hoy a un repostaje de hace un mes falsearía la comparación.
   */
  actualizarRepostaje(id: string, repostaje: NuevoRepostaje): Observable<Repostaje> {
    return this.http
      .put<RespuestaApi<Repostaje>>(`${this.base()}/repostajes/${id}`, repostaje)
      .pipe(map((respuesta) => respuesta.data));
  }

  eliminarRepostaje(id: string): Observable<void> {
    return this.http.delete<RespuestaApi<void>>(`${this.base()}/repostajes/${id}`).pipe(map(() => undefined));
  }
}
