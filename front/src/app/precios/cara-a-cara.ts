import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { NOMBRE_CADENA } from '../tickets/ticket.model';
import {
  GenericoDetalle,
  PrecioTienda,
  comoSeDice,
  diferenciaPorUnidad,
  precioPorUnidad,
  tamano,
  diaCorto,
} from './generico.model';

/** Los pasteles de los cromos, en ciclo (como las filas de miembros). */
const TONOS = ['var(--rubor)', 'var(--cielo)', 'var(--mantequilla)', 'var(--lila)'];

/**
 * El cara a cara: el producto más barato de cada cadena como un cromo, en orden de precio
 * por unidad. El primero lleva el check y la cinta encendida. Los cromos salen en abanico.
 * El precio es el de la web de la cadena (de hoy) o, si no la hay, el del último ticket: el
 * pie dice de dónde sale.
 */
@Component({
  selector: 'app-cara-a-cara',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let d = detalle();
    <ol class="cromos" [attr.aria-label]="'Dónde sale más barato ' + d.nombre">
      @for (c of cromos(); track c.clave; let i = $index) {
        <li
          class="cromo"
          [class.cromo--mejor]="c.mejor"
          [style.--tono]="tono(i)"
          [style.--inclina]="inclinacion(i)"
          [style.--retraso]="i * 70 + 'ms'"
        >
          <span class="cromo__cinta" aria-hidden="true">{{ c.puesto }}</span>
          <span class="cromo__tienda">{{ nombreCadena[c.producto.cadena] }}</span>
          <span class="cromo__nombre">{{ decir(c.producto.nombre) }}</span>
          <span class="cromo__precio">
            {{ precio(c.producto) }}
            @if (c.mejor) {
              <svg class="cromo__check" viewBox="0 0 16 16" role="img" aria-label="más barato">
                <path d="M3 8.5l3 3 7-7" />
              </svg>
            }
          </span>
          <span class="cromo__pie">{{ pie(c.producto) }}</span>
          @if (c.producto.url) {
            <a class="cromo__ver" [href]="c.producto.url" target="_blank" rel="noopener">
              Ver en {{ nombreCadena[c.producto.cadena]
              }}<span class="sr-solo"> (se abre su web)</span>
            </a>
          }
        </li>
      }
    </ol>
    @if (nota(); as n) {
      <p class="cara__nota">{{ n }}</p>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .cromos {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 18px 14px;
      margin: 0;
      padding: 10px 2px 2px;
      list-style: none;
    }

    .cromo {
      position: relative;
      display: grid;
      justify-items: center;
      align-content: start;
      gap: 6px;
      padding: 18px 10px 12px;
      border: 3px solid var(--papel);
      border-radius: var(--radio-ficha);
      background: var(--tono);
      box-shadow: var(--sombra-ficha);
      text-align: center;
      transform: rotate(var(--inclina));
      animation: abanico 560ms var(--salida) var(--retraso) backwards;
    }

    .cromo__cinta {
      position: absolute;
      top: -6px;
      left: 12px;
      display: grid;
      place-items: start center;
      width: 32px;
      height: 40px;
      padding-top: 5px;
      background: color-mix(in srgb, var(--cinta-listillo) 38%, var(--papel));
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1.15rem;
      line-height: 1;
      clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 80%, 0 100%);
    }

    .cromo--mejor .cromo__cinta {
      background: var(--cinta-listillo);
    }

    /* A la derecha de la cinta, para que el número no pise la tienda */
    .cromo__tienda {
      justify-self: stretch;
      padding-left: 34px;
      font-weight: 800;
      font-size: 0.8125rem;
      letter-spacing: 0.14em;
      text-transform: uppercase;
    }

    /* Si el nombre parte en varias líneas, la placa deja de ser píldora: radio de nota */
    .cromo__nombre {
      padding: 4px 10px;
      border-radius: 14px;
      background: var(--placa-nombre-lila);
      font-weight: 800;
      font-size: 0.875rem;
      line-height: 1.25;
      text-wrap: balance;
    }

    .cromo:nth-child(even) .cromo__nombre {
      background: var(--placa-nombre-rosa);
    }

    .cromo__marca {
      color: var(--tinta-suave);
      font-size: 0.8125rem;
    }

    .cromo__precio {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 12px;
      border-radius: var(--radio-placa);
      background: var(--placa-precio);
      font-weight: 800;
      font-size: 1rem;
    }

    .cromo__check {
      width: 16px;
      height: 16px;
      fill: none;
      stroke: var(--tinta);
      stroke-width: 2.6;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    /* Abrir la ficha en la web de la tienda: enlace de texto con subrayado rosa (DESIGN) */
    .cromo__ver {
      display: inline-flex;
      align-items: center;
      min-height: 36px;
      font-weight: 800;
      font-size: 0.8125rem;
      text-decoration: underline;
      text-decoration-color: var(--rosa-boton);
      text-decoration-thickness: 2px;
      text-underline-offset: 3px;
    }

    .cromo__pie {
      text-wrap: balance;
      color: var(--tinta-suave);
      font-size: 0.8125rem;
      line-height: 1.3;
    }

    /* El ganador da el saltito del catálogo cuando ya ha salido */
    .cromo--mejor {
      animation:
        abanico 560ms var(--salida) var(--retraso) backwards,
        salta 520ms var(--salida) 520ms;
    }

    /* La nota de Listillo, en placa mantequilla como las del catálogo */
    .cara__nota {
      width: fit-content;
      margin: 14px auto 0;
      padding: 6px 14px;
      border-radius: 12px;
      background: var(--placa-nota);
      font-size: 0.875rem;
      text-align: center;
      text-wrap: balance;
    }

    /* Salen de detrás de la fila: recogidos y rectos, hasta su sitio y su inclinación */
    @keyframes abanico {
      from {
        opacity: 0;
        transform: translateY(-26px) rotate(0deg) scale(0.9);
        clip-path: inset(0 0 70% 0 round 22px);
      }
      to {
        clip-path: inset(-20px -20px -20px -20px round 22px);
      }
    }

    @keyframes salta {
      40% {
        transform: rotate(var(--inclina)) translateY(-8px) scale(1.04, 0.97);
      }
      70% {
        transform: rotate(var(--inclina)) translateY(1px) scale(0.99, 1.02);
      }
    }
  `,
})
export class CaraACara {
  readonly detalle = input.required<GenericoDetalle>();

  protected readonly nombreCadena = NOMBRE_CADENA;

  /** Lo más barato de cada cadena, en el orden que da el back (de más barata a más cara). */
  protected readonly cromos = computed(() => {
    const tiendas = this.detalle().tiendas;
    return tiendas.map((producto) => ({
      clave: producto.cadena,
      producto,
      // Mismo precio, mismo puesto: en un empate ganan todos.
      puesto: tiendas.findIndex((o) => o.precio === producto.precio) + 1,
      mejor:
        tiendas.length > 1 &&
        producto.precio === tiendas[0].precio &&
        tiendas.at(-1)!.precio !== tiendas[0].precio,
    }));
  });

  /**
   * La nota de Listillo bajo los cromos: quién gana (o quiénes empatan) y cuánto se ahorra
   * frente al más caro.
   */
  protected readonly nota = computed(() => {
    const c = this.cromos();
    if (c.length === 0) return null;
    const nombre = (x: { producto: PrecioTienda }) => NOMBRE_CADENA[x.producto.cadena];
    if (c.length === 1) {
      return `Solo hay precio de ${nombre(c[0])}. Con un ticket de otra tienda, aquí se enfrentan.`;
    }
    const unidad = this.detalle().unidad;
    const mejor = c[0].producto.precio;
    const peor = c[c.length - 1].producto.precio;
    const dif = peor - mejor;
    if (dif <= 0) return `Todos a ${precioPorUnidad(mejor, unidad)}: aquí da igual dónde.`;
    // Los más caros también pueden empatar: se nombran todos.
    const caros = c.filter((x) => x.producto.precio === peor).map(nombre);
    const ahorro = `${diferenciaPorUnidad(dif, unidad)} frente a ${enumerar(caros)}`;
    const ganadores = c.filter((x) => x.mejor).map(nombre);
    if (ganadores.length > 1) {
      return `${enumerar(ganadores)} empatan a ${precioPorUnidad(mejor, unidad)}: te ahorras ${ahorro}.`;
    }
    return `En ${ganadores[0]} te ahorras ${ahorro}.`;
  });

  protected readonly decir = comoSeDice;

  protected precio(p: PrecioTienda): string {
    return precioPorUnidad(p.precio, this.detalle().unidad);
  }

  /** «1 L · 0,85 € · web» o «6 L · 5,34 € · ticket 7 oct». */
  protected pie(p: PrecioTienda): string {
    const partes = [];
    if (p.tamano !== null) partes.push(tamano(p.tamano, this.detalle().unidad));
    partes.push(`${p.importe.toLocaleString('es-ES', { minimumFractionDigits: 2 })} €`);
    partes.push(
      p.fuente === 'WEB'
        ? 'web'
        : p.fuente === 'OPEN_PRICES'
          ? `Open Prices ${diaCorto(p.fecha)}`
          : `ticket ${diaCorto(p.fecha)}`,
    );
    // Cada dato entero en su línea: que «5,70» no se separe de su «€».
    return partes.map((x) => x.replaceAll(' ', '\u00a0')).join('\u00a0· ');
  }

  protected tono(i: number): string {
    return TONOS[i % TONOS.length];
  }

  protected inclinacion(i: number): string {
    return ['-1.5deg', '1deg', '1deg', '-1.5deg'][i % 4];
  }
}

/** «Mercadona», «Mercadona y Dia», «Lidl, Aldi y Dia». */
function enumerar(nombres: string[]): string {
  return nombres.length < 2
    ? (nombres[0] ?? '')
    : `${nombres.slice(0, -1).join(', ')} y ${nombres.at(-1)}`;
}
