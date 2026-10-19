import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Una capa del tarro: un pasillo con su importe y su color. */
export interface Capa {
  clave: string;
  nombre: string;
  total: number;
  color: string;
}

/** Medidas del tarro dentro del viewBox (0 0 200 250). */
const FONDO = 232;
const BOCA = 64;
const ALTO = FONDO - BOCA;

/**
 * El tarro de la hucha, dibujado como los personajes: cristal con brillo, tapa con ranura y
 * las capas de cada pasillo apiladas de abajo arriba. Lo lleno que está dice cuánto se ha
 * gastado este mes frente al mes más caro de los que se ven.
 *
 * Las capas caen y se apilan al cambiar de mes (se recrean: la clave lleva el mes).
 */
@Component({
  selector: 'app-hucha',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg class="hucha" viewBox="0 0 200 250" role="img" [attr.aria-label]="etiqueta()">
      <defs>
        <clipPath [attr.id]="'vidrio-' + id()">
          <rect x="34" y="62" width="132" height="174" rx="30" />
        </clipPath>
      </defs>
      <ellipse cx="100" cy="242" rx="70" ry="7" fill="#2A2F5A" opacity=".12" />
      <!-- El cristal -->
      <rect x="28" y="56" width="144" height="186" rx="36" fill="#FFFFFF" />
      <rect x="34" y="62" width="132" height="174" rx="30" fill="#F7F5FC" />
      <!-- Las capas, dentro del cristal -->
      <g [attr.clip-path]="'url(#vidrio-' + id() + ')'">
        @for (c of capas(); track mes() + c.clave; let i = $index) {
          <rect
            class="capa"
            x="34"
            [attr.y]="c.y"
            width="132"
            [attr.height]="c.alto"
            [attr.fill]="c.color"
            [style.--retraso]="i * 90 + 'ms'"
          />
        }
        <!-- Los filetes van después de todas las capas: si no, cada capa tapa el de debajo -->
        @for (c of capas(); track mes() + c.clave; let i = $index) {
          <line class="capa capa__filete" x1="34" x2="166" [attr.y1]="c.y" [attr.y2]="c.y" [style.--retraso]="i * 90 + 'ms'" />
        }
      </g>
      <!-- Brillo del cristal -->
      <path d="M48 96 Q48 78 62 72" stroke="#FFFFFF" stroke-width="8" fill="none" stroke-linecap="round" opacity=".9" />
      <path d="M46 116 V150" stroke="#FFFFFF" stroke-width="8" fill="none" stroke-linecap="round" opacity=".7" />
      <!-- La tapa, con su ranura para las monedas -->
      <rect x="44" y="30" width="112" height="30" rx="12" fill="#FFB23E" />
      <rect x="44" y="30" width="112" height="11" rx="5.5" fill="#FFCB73" />
      <rect x="78" y="40" width="44" height="8" rx="4" fill="#2A2F5A" />
    </svg>
  `,
  styles: `
    :host { display: block; }

    .hucha {
      display: block;
      width: 100%;
      height: auto;
      overflow: visible;
    }

    /* Caen desde arriba de la boca y se asientan en su sitio con un rebote corto */
    .capa {
      animation: cae 620ms var(--salida) var(--retraso) backwards;
    }

    .capa__filete {
      stroke: #fff;
      stroke-width: 3;
    }

    @keyframes cae {
      0% {
        opacity: 0;
        transform: translateY(-150px);
      }
      70% {
        opacity: 1;
        transform: translateY(3px);
      }
    }
  `,
})
export class Hucha {
  /** Para que cada tarro tenga su recorte. */
  readonly id = input.required<string>();
  /** Al cambiar, las capas vuelven a caer. */
  readonly mes = input.required<string>();
  readonly capasDeEntrada = input.required<readonly Capa[]>({ alias: 'capas' });
  /** De 0 a 1: lo lleno que está el tarro. */
  readonly nivel = input.required<number>();
  readonly etiqueta = input.required<string>();

  /** La primera capa abajo del todo; cada una con su alto proporcional. */
  protected readonly capas = computed(() => {
    const capas = this.capasDeEntrada();
    const suma = capas.reduce((s, c) => s + c.total, 0);
    const lleno = ALTO * Math.min(1, Math.max(0, this.nivel()));
    let base = FONDO + 4; // un poco por debajo: el redondeo del cristal la recorta
    return capas.map((c) => {
      const alto = suma > 0 ? (lleno * c.total) / suma : 0;
      base -= alto;
      return { ...c, y: base, alto: alto + 4 };
    });
  });
}
