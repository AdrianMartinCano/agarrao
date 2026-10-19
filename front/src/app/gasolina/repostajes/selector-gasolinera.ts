import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { Coordenadas, EstacionCercana, Lugar } from '../core/api.model';
import { EstacionesService } from '../core/estaciones.service';
import { GeolocalizacionService, POSICION_POR_DEFECTO } from '../core/geolocalizacion.service';
import { BuscadorLugar } from '../buscador/buscador-lugar';

/**
 * Elige en qué gasolinera se ha repostado.
 *
 * DE ENTRADA MUESTRA LAS DE ALREDEDOR, que es lo cómodo cuando se apunta el repostaje
 * recién hecho. Pero un repostaje se registra a menudo días después y desde otro sitio
 * —al volver de un viaje, por ejemplo—, y entonces la gasolinera buena no está entre las
 * cercanas. Por eso se puede cambiar el sitio donde buscar por municipio o código postal,
 * reutilizando el mismo buscador del que usa la portada.
 *
 * Al elegir una, se emite con su precio de HOY para ese carburante y el formulario lo
 * rellena solo. Ojo: es el precio de hoy, no el del día del repostaje. Para uno reciente
 * es exacto; para uno de hace semanas es un punto de partida, y por eso el campo del
 * precio sigue siendo editable.
 */
@Component({
  selector: 'app-selector-gasolinera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BuscadorLugar],
  template: `
    @if (elegida(); as estacion) {
      <div class="elegida">
        <div class="elegida__texto">
          <strong>{{ estacion.rotulo }}</strong>
          <span>{{ estacion.direccion }}, {{ estacion.municipio }}</span>
        </div>
        <button type="button" class="cambiar" (click)="limpiar()">Cambiar</button>
      </div>
    } @else {
      <div class="buscar">
        @if (buscandoEnOtroSitio()) {
          <app-buscador-lugar (lugarElegido)="cambiarLugar($event)" />
        } @else {
          <p class="donde">
            <span>{{ dondeSeBusca() }}</span>
            <!-- El caso que esto resuelve: apuntar hoy un repostaje de otro sitio. -->
            <button type="button" class="enlace" (click)="buscandoEnOtroSitio.set(true)">
              ¿Fue en otro sitio?
            </button>
          </p>
        }
      </div>

      @if (cercanas.isLoading()) {
        <p class="mensaje" role="status">Buscando gasolineras…</p>
      } @else if (cercanas.value()?.contenido; as estaciones) {
        @if (estaciones.length === 0) {
          <p class="mensaje">
            No hay gasolineras con {{ carburanteEtiqueta() }} por ahí. Busca en otro sitio, o
            deja la gasolinera en blanco y escribe el precio a mano.
          </p>
        } @else {
          <ul class="opciones">
            @for (estacion of estaciones; track estacion.id) {
              <li>
                <button type="button" (click)="elegir(estacion)">
                  <span class="opcion__texto">
                    <span class="opcion__rotulo">{{ estacion.rotulo }}</span>
                    <span class="opcion__direccion">
                      {{ estacion.direccion }}
                      @if (estacion.distanciaKm !== null) {
                        · {{ estacion.distanciaKm.toFixed(1) }} km
                      }
                    </span>
                  </span>
                  @if (estacion.precio !== null) {
                    <span class="opcion__precio">{{ estacion.precio.toFixed(3) }} €</span>
                  }
                </button>
              </li>
            }
          </ul>
        }
      }
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .buscar {
      margin-block-end: 0.5rem;
    }

    .donde {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 0.5rem;
      margin: 0;
      font-size: 0.8rem;
      color: var(--tinta-suave);
    }

    .enlace {
      padding: 0;
      border: none;
      background: none;
      color: var(--azul);
      font: inherit;
      text-decoration: underline;
      cursor: pointer;
    }

    .opciones {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      max-block-size: 13rem;
      overflow-y: auto;

      button {
        inline-size: 100%;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding: 0.5rem 0.6rem;
        border: 1px solid var(--borde);
        border-radius: 0.45rem;
        background: var(--superficie);
        color: inherit;
        font: inherit;
        text-align: start;
        cursor: pointer;

        &:hover {
          border-color: var(--azul);
        }
      }
    }

    .opcion__texto {
      display: flex;
      flex-direction: column;
      min-inline-size: 0;
    }

    .opcion__rotulo {
      font-weight: 700;
      font-size: 0.9rem;
    }

    .opcion__direccion {
      font-size: 0.78rem;
      color: var(--tinta-suave);
    }

    .opcion__precio {
      font-weight: 800;
      letter-spacing: -0.02em;
      color: var(--ambar);
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
    }

    .elegida {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.6rem 0.7rem;
      border: 1px solid var(--azul);
      border-radius: 0.45rem;
      background: var(--azul-claro);
    }

    .elegida__texto {
      display: flex;
      flex-direction: column;
      min-inline-size: 0;
      font-size: 0.85rem;

      span {
        color: var(--tinta-suave);
        font-size: 0.78rem;
      }
    }

    .cambiar {
      padding: 0.3rem 0.6rem;
      border: 1px solid var(--borde);
      border-radius: 0.4rem;
      background: var(--superficie);
      color: var(--tinta);
      font: inherit;
      font-size: 0.8rem;
      cursor: pointer;
      white-space: nowrap;
    }

    .mensaje {
      margin: 0;
      padding: 0.6rem 0;
      font-size: 0.85rem;
      color: var(--tinta-suave);
      line-height: 1.5;
    }
  `,
})
export class SelectorGasolinera {
  private readonly estacionesService = inject(EstacionesService);
  private readonly geolocalizacion = inject(GeolocalizacionService);
  private readonly destroyRef = inject(DestroyRef);

  /** Carburante del vehículo: filtra la lista y decide qué precio se ofrece. */
  readonly carburante = input.required<string>();
  readonly carburanteEtiqueta = input('ese carburante');

  readonly estacionElegida = output<EstacionCercana | null>();

  protected readonly elegida = signal<EstacionCercana | null>(null);
  protected readonly buscandoEnOtroSitio = signal(false);

  private readonly posicion = signal<Coordenadas>(POSICION_POR_DEFECTO);
  private readonly ubicacionReal = signal(false);
  private readonly lugar = signal<string | null>(null);

  /** Qué se le enseña al usuario sobre dónde se está buscando. */
  protected readonly dondeSeBusca = computed(() => {
    const lugar = this.lugar();
    if (lugar) {
      return `Gasolineras en ${lugar}`;
    }
    return this.ubicacionReal() ? 'Gasolineras cerca de ti' : 'Gasolineras del centro de Madrid';
  });

  /**
   * Las del punto elegido, ya con el precio del carburante del vehículo.
   *
   * El radio es mayor que en la portada: aquí no se busca dónde repostar ahora, se busca
   * una gasolinera concreta que ya se conoce, y puede estar a las afueras del municipio.
   */
  protected readonly cercanas = rxResource({
    params: () => ({ posicion: this.posicion(), carburante: this.carburante() }),
    stream: ({ params }) =>
      this.estacionesService
        .cercanas(
          params.posicion,
          { carburante: params.carburante, radioKm: 15, orden: 'DISTANCIA' },
          0,
          25,
        )
        .pipe(
          // Aquí solo interesa la lista para elegir dónde repostaste: el resumen de
          // precios de la zona es cosa del buscador, no de este desplegable.
          map((resultado) => resultado.estaciones),
          catchError(() => of({ contenido: [], pagina: 0, tamano: 0, total: 0, totalPaginas: 0 })),
        ),
  });

  constructor() {
    this.geolocalizacion
      .posicionActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (posicion) => {
          this.posicion.set(posicion);
          this.ubicacionReal.set(true);
        },
        // Sin ubicación se busca en el punto por defecto y siempre se puede cambiar de
        // sitio a mano, así que no hay nada que avisar aquí.
        error: () => undefined,
      });
  }

  protected cambiarLugar(lugar: Lugar): void {
    this.posicion.set({ latitud: lugar.latitud, longitud: lugar.longitud });
    this.lugar.set(`${lugar.nombre} (${lugar.detalle})`);
    this.buscandoEnOtroSitio.set(false);
  }

  protected elegir(estacion: EstacionCercana): void {
    this.elegida.set(estacion);
    this.estacionElegida.emit(estacion);
  }

  protected limpiar(): void {
    this.elegida.set(null);
    this.estacionElegida.emit(null);
  }
}
