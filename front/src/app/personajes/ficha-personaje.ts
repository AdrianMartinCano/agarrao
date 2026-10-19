import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

/** Una línea de comparación dentro de la ficha: el mismo genérico en otra tienda. */
export interface PrecioComparado {
  /** Quién lo vende. En la portada son «Súper A/B»: precios de ejemplo, sin cadenas reales. */
  donde: string;
  precio: string;
  /** El más barato de la ficha. */
  mejor?: boolean;
}

/** Un personaje del elenco: un producto genérico con su ficha. */
export interface Personaje {
  /** Su número en el elenco: va en la cinta. */
  puesto: number;
  nombre: string;
  /** Precio por unidad, ya formateado («0,85 €/L»). Null = todavía sin precio. */
  precio: string | null;
  /** Texto en la placa del precio para quien no es un producto (Listillo). */
  etiqueta?: string;
  /** Si viene, la placa del precio enseña la comparación en vez de un precio suelto. */
  comparativa?: readonly PrecioComparado[];
  nota: string;
  /** Fichero en public/personajes. */
  imagen: string;
  /** Pastel de fondo de la ficha. */
  tono: 'lila' | 'rubor' | 'menta' | 'cielo' | 'mantequilla';
  /** Color de su cinta. */
  cinta: 'listillo' | 'leche' | 'huevo' | 'tomate';
  /** Color de la placa del nombre: se alternan, como en el catálogo. */
  placa: 'lila' | 'rosa';
}

/**
 * La ficha de un personaje, como en un catálogo de muñecos: cinta numerada, retrato,
 * placa con el nombre, placa con el precio por unidad y una línea de carácter.
 *
 * Se puede tocar: el personaje da un saltito y su cinta toma su color, como el muñeco
 * escogido en el catálogo. Sin precio la ficha sale en gris, como un muñeco retirado.
 */
@Component({
  selector: 'app-ficha-personaje',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'ficha',
    '[class.ficha--sin-precio]': 'sinPrecio()',
    '[class.ficha--elegida]': 'elegida()',
    '[style.--ficha-tono]': 'tono()',
    '[style.--ficha-cinta]': 'cinta()',
    '[style.--ficha-placa]': 'placa()',
  },
  template: `
    @let p = personaje();
    <button type="button" class="ficha__toque" [attr.aria-pressed]="elegida()" (click)="tocar()">
      <span class="ficha__cinta" aria-hidden="true">{{ p.puesto }}</span>
      <img
        class="ficha__retrato"
        [class.ficha__retrato--salta]="saltando()"
        [src]="'personajes/' + p.imagen"
        alt=""
        width="160"
        height="160"
        (animationend)="saltando.set(false)"
      />
      <span class="ficha__nombre">{{ p.nombre }}</span>
      @if (p.comparativa; as filas) {
        <span class="ficha__comparativa">
          @for (fila of filas; track fila.donde) {
            <span class="ficha__fila" [class.ficha__fila--mejor]="fila.mejor">
              <span>{{ fila.donde }}</span>
              <span>
                {{ fila.precio }}
                @if (fila.mejor) {
                  <svg class="ficha__check" viewBox="0 0 16 16" role="img" aria-label="más barato">
                    <path d="M3 8.5l3 3 7-7" />
                  </svg>
                }
              </span>
            </span>
          }
        </span>
      } @else {
        <span class="ficha__precio">
          @if (p.etiqueta) {
            {{ p.etiqueta }}
          } @else if (p.precio) {
            <span class="sr-solo">Precio por unidad: </span>{{ p.precio }}
          } @else {
            Sin precio aún
          }
        </span>
      }
      <span class="ficha__nota">{{ p.nota }}</span>
    </button>
  `,
  styles: `
    :host {
      display: block;
    }

    .ficha__toque {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      width: 100%;
      height: 100%;
      padding: 14px 10px 12px;
      border-radius: var(--radio-ficha);
      background: var(--ficha-tono);
      border: 3px solid var(--papel);
      box-shadow: var(--sombra-ficha);
      color: var(--tinta);
      font: inherit;
      text-align: center;
      cursor: pointer;
      -webkit-tap-highlight-color: transparent;
      transition: transform 380ms var(--salida), box-shadow 380ms var(--salida);
    }

    .ficha__toque:hover {
      transform: translateY(-3px);
      box-shadow: 0 12px 24px -8px rgb(42 47 90 / 0.3), 0 2px 4px rgb(42 47 90 / 0.08);
    }

    .ficha__toque:active {
      transform: translateY(1px) scale(0.985);
    }

    /* La cinta: apagada en reposo, de su color cuando se elige o se señala */
    .ficha__cinta {
      position: absolute;
      top: -6px;
      left: 12px;
      display: grid;
      place-items: start center;
      width: 32px;
      height: 40px;
      padding-top: 5px;
      background: color-mix(in srgb, var(--ficha-cinta) 38%, var(--papel));
      color: var(--tinta);
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1.15rem;
      line-height: 1;
      clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 80%, 0 100%);
      transition: background 260ms ease;
    }

    :host(.ficha--elegida) .ficha__cinta,
    .ficha__toque:hover .ficha__cinta,
    .ficha__toque:focus-visible .ficha__cinta {
      background: var(--ficha-cinta);
    }

    .ficha__retrato {
      width: 100%;
      max-width: var(--retrato, 112px);
      height: auto;
      aspect-ratio: 1;
      margin-top: 2px;
      transform-origin: 50% 90%;
    }

    .ficha__retrato--salta {
      animation: salta 520ms var(--salida);
    }

    .ficha__nombre,
    .ficha__precio,
    .ficha__nota,
    .ficha__comparativa {
      display: block;
      max-width: 100%;
    }

    .ficha__nombre {
      padding: 4px 12px;
      border-radius: var(--radio-placa);
      background: var(--ficha-placa);
      font-weight: 800;
      font-size: 0.95rem;
      line-height: 1.25;
    }

    .ficha__precio {
      padding: 2px 12px;
      border-radius: var(--radio-placa);
      background: var(--placa-precio);
      font-weight: 800;
      font-size: 0.875rem;
    }

    .ficha__comparativa {
      display: grid;
      gap: 3px;
      width: 100%;
    }

    .ficha__fila {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 4px;
      padding: 2px 7px;
      border-radius: var(--radio-placa);
      white-space: nowrap;
      font-size: 0.78rem;
      background: color-mix(in srgb, var(--placa-precio) 45%, var(--papel));
      font-weight: 500;
    }

    .ficha__fila--mejor {
      background: var(--placa-precio);
      font-weight: 800;
    }

    .ficha__check {
      width: 14px;
      height: 14px;
      margin-left: 2px;
      vertical-align: -2px;
      fill: none;
      stroke: var(--tinta);
      stroke-width: 2.6;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    .ficha__nota {
      padding: 4px 10px;
      border-radius: 12px;
      background: var(--placa-nota);
      font-size: 0.8125rem;
      line-height: 1.3;
    }

    :host(.ficha--sin-precio) .ficha__toque {
      filter: grayscale(1);
    }

    .sr-solo {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip-path: inset(50%);
      white-space: nowrap;
    }

    @keyframes salta {
      30% { transform: translateY(-14px) scale(1.04, 0.97); }
      60% { transform: translateY(0) scale(0.97, 1.03); }
    }
  `,
})
export class FichaPersonaje {
  readonly personaje = input.required<Personaje>();
  readonly elegida = input(false);
  readonly elegir = output<number>();

  protected readonly saltando = signal(false);

  protected readonly sinPrecio = computed(() => {
    const p = this.personaje();
    return !p.precio && !p.etiqueta && !p.comparativa;
  });
  protected readonly tono = computed(() => `var(--${this.personaje().tono})`);
  protected readonly cinta = computed(() => `var(--cinta-${this.personaje().cinta})`);
  protected readonly placa = computed(() => `var(--placa-nombre-${this.personaje().placa})`);

  protected tocar(): void {
    this.saltando.set(true);
    this.elegir.emit(this.personaje().puesto);
  }
}
