import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { Coordenadas, EstacionCercana, GasolineraEnRuta, Lugar, Viaje } from '../core/api.model';
import { urlViajeConParada } from '../core/como-llegar';
import { EstacionesService } from '../core/estaciones.service';
import { FiltrosStore } from '../core/filtros.store';
import { GeolocalizacionService } from '../core/geolocalizacion.service';
import { mensajeDeErrorHttp } from '../core/mensaje-error';
import { BuscadorLugar } from '../buscador/buscador-lugar';
import { MapaEstaciones } from '../buscador/mapa-estaciones';

/** Un extremo del viaje: dónde está y cómo se llama, para decirlo en pantalla. */
interface Punto extends Coordenadas {
  nombre: string;
}

/** Cuánto puede uno salirse de la carretera para repostar (en línea recta). */
export const DESVIOS: readonly number[] = [1, 2, 5, 10];

/** Para el «te ahorras» de cada gasolinera: un depósito normal. */
const LITROS_DEPOSITO = 50;

/**
 * Viajes: de dónde sales, adónde vas, y dónde repostar más barato sin salirte del camino.
 *
 * Público, como el buscador. La ruta la calcula el back (OSRM) y aquí solo se pinta: la
 * línea en el mapa y las gasolineras de camino, de la más barata a la más cara, cada una
 * con su kilómetro y un enlace a Google Maps con el viaje y la parada.
 */
@Component({
  selector: 'app-viajes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BuscadorLugar, MapaEstaciones],
  templateUrl: './viajes.page.html',
  styleUrl: './viajes.page.scss',
})
export class ViajesPage {
  private readonly estaciones = inject(EstacionesService);
  private readonly geolocalizacion = inject(GeolocalizacionService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly campoOrigen = viewChild.required<BuscadorLugar>('campoOrigen');
  private readonly campoDestino = viewChild.required<BuscadorLugar>('campoDestino');

  protected readonly desvios = DESVIOS;

  protected readonly carburantes = toSignal(this.estaciones.carburantes().pipe(catchError(() => of([]))), {
    initialValue: [],
  });

  protected readonly origen = signal<Punto | null>(null);
  protected readonly destino = signal<Punto | null>(null);
  /** El del buscador, para no tener que elegirlo dos veces. */
  protected readonly carburante = signal(inject(FiltrosStore).filtros().carburante ?? 'GASOLINA_95_E5');
  protected readonly desvioKm = signal(2);

  protected readonly buscando = signal(false);
  protected readonly localizando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly viaje = signal<Viaje | null>(null);
  protected readonly seleccionada = signal<string | null>(null);

  protected readonly listo = computed(() => !!this.origen() && !!this.destino() && !this.buscando());

  protected readonly estacionesMapa = computed<EstacionCercana[]>(
    () => this.viaje()?.estaciones.map((g) => g.estacion) ?? [],
  );

  protected elegirOrigen(lugar: Lugar): void {
    this.origen.set({ latitud: lugar.latitud, longitud: lugar.longitud, nombre: lugar.nombre });
  }

  protected elegirDestino(lugar: Lugar): void {
    this.destino.set({ latitud: lugar.latitud, longitud: lugar.longitud, nombre: lugar.nombre });
  }

  /** Salir desde donde estás: lo normal al arrancar un viaje con el móvil en la mano. */
  protected desdeAqui(): void {
    this.localizando.set(true);
    this.error.set(null);
    this.geolocalizacion
      .posicionActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (posicion) => {
          this.origen.set({ ...posicion, nombre: 'Mi ubicación' });
          this.campoOrigen().poner('Mi ubicación');
          this.localizando.set(false);
        },
        error: (error: Error) => {
          this.error.set(error.message);
          this.localizando.set(false);
        },
      });
  }

  /** La vuelta: el mismo viaje al revés. */
  protected darLaVuelta(): void {
    const origen = this.origen();
    const destino = this.destino();
    this.origen.set(destino);
    this.destino.set(origen);
    this.campoOrigen().poner(destino?.nombre ?? '');
    this.campoDestino().poner(origen?.nombre ?? '');
    if (this.viaje()) {
      this.buscar();
    }
  }

  protected buscar(): void {
    const origen = this.origen();
    const destino = this.destino();
    if (!origen || !destino) {
      return;
    }
    this.buscando.set(true);
    this.error.set(null);
    this.seleccionada.set(null);
    this.estaciones
      .ruta(origen, destino, this.carburante(), this.desvioKm())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (viaje) => {
          this.viaje.set(viaje);
          this.buscando.set(false);
        },
        error: (error: unknown) => {
          this.viaje.set(null);
          this.error.set(mensajeDeErrorHttp(error, 'No se ha podido calcular el viaje. Prueba otra vez.'));
          this.buscando.set(false);
        },
      });
  }

  protected conParada(g: GasolineraEnRuta): string {
    return urlViajeConParada(this.origen()!, this.destino()!, g.estacion);
  }

  /** «3 h 5 min», «45 min». */
  protected duracion(minutos: number): string {
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    return horas === 0 ? `${resto} min` : `${horas} h ${resto} min`;
  }

  /** Lo que se ahorra un depósito de 50 l frente a la media de la ruta (0 si no ahorra). */
  protected ahorro(g: GasolineraEnRuta): number {
    const media = this.viaje()?.mediaRuta;
    if (media == null || g.estacion.precio == null) return 0;
    return Math.max(0, (media - g.estacion.precio) * LITROS_DEPOSITO);
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected km(valor: number): string {
    return valor.toLocaleString('es-ES', { maximumFractionDigits: 1 });
  }
}
