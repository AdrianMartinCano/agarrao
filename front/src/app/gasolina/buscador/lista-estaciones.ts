import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { EstacionCercana } from '../core/api.model';
import { urlComoLlegar } from '../core/como-llegar';

/**
 * Lista de resultados. Componente presentacional: solo pinta lo que recibe y avisa de
 * los clics; no sabe de HTTP ni de sesiones.
 */
@Component({
  selector: 'app-lista-estaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="lista">
      @for (estacion of estaciones(); track estacion.id) {
        <li
          class="tarjeta"
          [class.tarjeta--activa]="estacion.id === seleccionada()"
          [class.tarjeta--favorita]="favoritos().has(estacion.id)"
        >
          <button type="button" class="tarjeta__cuerpo" (click)="estacionElegida.emit(estacion)">
            <div class="tarjeta__cabecera">
              <span class="rotulo">{{ estacion.rotulo || 'Sin rótulo' }}</span>
              @if (estacion.precio !== null) {
                <span class="precio-bloque">
                  <span class="precio">{{ estacion.precio.toFixed(3) }}</span>
                  <span class="precio-unidad">€/l</span>
                </span>
              }
            </div>

            <p class="direccion">{{ estacion.direccion }}</p>

            @if (preciosDestacados().length > 0) {
              <ul class="precios">
                @for (precio of preciosDe(estacion); track precio.etiqueta) {
                  <li>
                    <span class="precios__nombre">{{ precio.etiqueta }}</span>
                    <span class="precios__valor" [class.precios__valor--sin]="precio.valor === null">
                      {{ precio.valor === null ? '—' : precio.valor.toFixed(3) + ' €' }}
                    </span>
                  </li>
                }
              </ul>
            }

            <p class="detalles">
              @if (estacion.distanciaKm !== null) {
                <span class="distancia">{{ formatearDistancia(estacion.distanciaKm) }}</span>
              }
              <span class="municipio">{{ estacion.municipio }}</span>
              @if (estacion.horario) {
                <span class="horario">{{ estacion.horario }}</span>
              }
            </p>
          </button>

          <a
            class="como-llegar"
            [href]="comoLlegar(estacion)"
            target="_blank"
            rel="noopener noreferrer"
            [attr.aria-label]="'Cómo llegar a ' + estacion.rotulo + ', ' + estacion.direccion"
            title="Abrir la ruta en Google Maps"
          >
            ➜
          </a>

          @if (mostrarFavoritos()) {
            <button
              type="button"
              class="favorito"
              [class.favorito--activo]="favoritos().has(estacion.id)"
              [attr.aria-pressed]="favoritos().has(estacion.id)"
              [attr.aria-label]="
                (favoritos().has(estacion.id) ? 'Quitar de favoritos: ' : 'Guardar en favoritos: ') +
                estacion.rotulo
              "
              (click)="favoritoAlternado.emit(estacion)"
            >
              {{ favoritos().has(estacion.id) ? '★' : '☆' }}
            </button>
          }
        </li>
      } @empty {
        <li class="vacio">{{ mensajeVacio() }}</li>
      }
    </ul>
  `,
  styles: `
    .lista {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .tarjeta {
      display: flex;
      align-items: stretch;
      gap: 0.25rem;
      background: var(--superficie);
      border: 1px solid var(--borde);
      border-radius: var(--radio);
      overflow: hidden;
      transition:
        border-color 0.15s ease,
        transform 0.15s ease;
    }

    .tarjeta:hover {
      border-color: var(--azul);
      transform: translateY(-1px);
    }

    .tarjeta--activa {
      border-color: var(--azul);
      box-shadow: 0 0 0 2px color-mix(in srgb, var(--azul) 22%, transparent);
    }

    /* Franja ámbar a la izquierda: se reconoce una favorita de un vistazo, sin tener que
       ir mirando la estrella de cada fila. Mismo color que la estrella y que su marcador
       en el mapa, para que las tres cosas se lean como lo mismo. */
    .tarjeta--favorita {
      border-inline-start: 3px solid var(--ambar);
    }

    .tarjeta__cuerpo {
      flex: 1;
      text-align: start;
      background: none;
      border: none;
      padding: 0.75rem 0.9rem;
      cursor: pointer;
      color: inherit;
      font: inherit;
    }

    .tarjeta__cabecera {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 1rem;
    }

    .rotulo {
      font-weight: 700;
      letter-spacing: -0.01em;
    }

    /* EL PRECIO ES LA FIRMA de la página: es a lo que se viene, y por eso se trata como
       el cartel de una gasolinera en la carretera — número grande y unidad pequeña al
       lado, no las dos cosas al mismo tamaño. */
    .precio-bloque {
      display: flex;
      align-items: baseline;
      gap: 0.15rem;
      white-space: nowrap;
      color: var(--ambar);
    }

    .precio {
      font-size: 1.45rem;
    }

    .precio-unidad {
      font-size: 0.72rem;
      font-weight: 700;
      opacity: 0.75;
    }

    .direccion {
      margin: 0.2rem 0 0;
      color: var(--tinta-suave);
      font-size: 0.9rem;
    }

    .precios {
      list-style: none;
      margin: 0.5rem 0 0;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem 1.25rem;

      li {
        display: flex;
        align-items: baseline;
        gap: 0.4rem;
        font-size: 0.85rem;
      }
    }

    .precios__nombre {
      color: var(--tinta-suave);
    }

    .precios__valor {
      font-weight: 700;
      color: var(--azul);
      font-variant-numeric: tabular-nums;
    }

    .precios__valor--sin {
      font-weight: 400;
      color: var(--tinta-suave);
    }

    .detalles {
      margin: 0.35rem 0 0;
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      font-size: 0.8rem;
      color: var(--tinta-suave);
    }

    .distancia {
      font-weight: 600;
      color: var(--tinta);
      font-variant-numeric: tabular-nums;
    }

    .como-llegar,
    .favorito {
      display: flex;
      align-items: center;
      justify-content: center;
      background: none;
      border: none;
      padding: 0 0.85rem;
      font-size: 1.35rem;
      line-height: 1;
      cursor: pointer;
      color: var(--tinta-suave);
      text-decoration: none;
    }

    .como-llegar {
      font-size: 1.15rem;

      &:hover {
        color: var(--azul);
      }
    }

    .favorito--activo {
      color: var(--ambar);
    }

    .vacio {
      padding: 2rem 1rem;
      text-align: center;
      color: var(--tinta-suave);
    }
  `,
})
export class ListaEstaciones {
  readonly estaciones = input.required<readonly EstacionCercana[]>();
  readonly favoritos = input<ReadonlySet<string>>(new Set());
  readonly seleccionada = input<string | null>(null);
  readonly mostrarFavoritos = input(false);
  readonly mensajeVacio = input('No hemos encontrado gasolineras con esos filtros.');

  /**
   * Carburantes cuyo precio se enseña en cada tarjeta.
   *
   * Vacío en el buscador, donde ya se ve el precio del carburante filtrado y repetirlo
   * sobraría. En favoritas, en cambio, no hay filtro: sin esto, la lista saldría sin un
   * solo precio, que es justo lo que se viene a mirar.
   */
  readonly preciosDestacados = input<readonly { clave: string; etiqueta: string }[]>([]);

  readonly estacionElegida = output<EstacionCercana>();
  readonly favoritoAlternado = output<EstacionCercana>();

  /** Por debajo de un kilómetro, los metros se leen mucho mejor que "0.35 km". */
  protected formatearDistancia(km: number): string {
    return km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1)} km`;
  }

  protected comoLlegar(estacion: EstacionCercana): string {
    return urlComoLlegar(estacion);
  }

  /** Precio de cada carburante destacado; null si esa estación no lo vende. */
  protected preciosDe(estacion: EstacionCercana): { etiqueta: string; valor: number | null }[] {
    return this.preciosDestacados().map((carburante) => ({
      etiqueta: carburante.etiqueta,
      valor: estacion.precios[carburante.clave] ?? null,
    }));
  }
}
