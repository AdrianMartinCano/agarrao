import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { GenericoResumen, NOMBRE_CATEGORIA, porUnidad } from '../precios/generico.model';
import { Unidad } from './ticket.model';

/** Lo que se elige en el panel: un genérico (con su tamaño, si se sabe) o ninguno. */
export interface Eleccion {
  genericoId: string | null;
  tamano: number | null;
}

/**
 * El panel bajo una línea del ticket guardado para decir qué genérico es y cuánto trae cada
 * unidad («6» L en «LECHE SEMI 6X1L»). Sin tamaño, Listillo lo lee del producto o del texto.
 */
@Component({
  selector: 'app-clasificar-linea',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="sr-solo" [for]="'generico-' + clave()">Qué genérico es {{ texto() }}</label>
    <input
      class="campo casar__campo"
      [id]="'generico-' + clave()"
      placeholder="Busca un genérico: leche, huevos…"
      autocomplete="off"
      [value]="busqueda()"
      (input)="busqueda.set(valor($event))"
    />
    @if (catalogo() === null) {
      <p class="clasificar__nota">Cargando los genéricos…</p>
    } @else {
      <div class="casar__opciones">
        @for (g of opciones(); track g.id) {
          <button
            type="button"
            class="casar__opcion"
            [class.casar__opcion--elegida]="g.id === elegido()?.id"
            [attr.aria-pressed]="g.id === elegido()?.id"
            (click)="elegir(g)"
          >
            {{ g.nombre }}
          </button>
        } @empty {
          <p class="clasificar__nota">Ninguno se llama así. Créalo desde Precios.</p>
        }
      </div>
    }

    @if (elegido(); as g) {
      @if (unidadLinea() === 'UD') {
        <label class="clasificar__tamano">
          <span>Cada unidad trae</span>
          <input
            class="campo clasificar__cifra"
            inputmode="decimal"
            placeholder="?"
            [value]="tamanoTexto()"
            (input)="tamanoTexto.set(valor($event))"
          />
          <span>{{ medida(g.unidad) }}</span>
        </label>
      }
      <p class="clasificar__nota">{{ nombreCategoria[g.categoria] }} · se compara en {{ porUnidad(g.unidad) }}</p>
    }

    @if (error()) {
      <p class="aviso-error" role="alert">{{ error() }}</p>
    }

    <div class="casar__acciones">
      <button type="button" class="casar__accion" (click)="elegido() ? quitarEleccion() : ninguno.emit()">
        {{ elegido() ? 'Cambiar' : 'No es ninguno' }}
      </button>
      <button type="button" class="boton-papel clasificar__guardar" [disabled]="!elegido() || ocupado()" (click)="guardar()">
        {{ ocupado() ? 'Guardando…' : 'Guardar' }}
      </button>
    </div>
  `,
  styles: `
    /* El mismo panel que el de casar en los tickets (tira.scss), con sus propias piezas */
    :host {
      grid-column: 1 / -1;
      display: grid;
      gap: 8px;
      margin: 6px 0 8px;
      padding: 10px;
      border-radius: 16px;
      background: var(--lila);
      animation: abre 320ms var(--salida) both;
    }

    .casar__campo { min-height: 46px; font-size: 0.9375rem; }

    .casar__opciones {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .casar__opcion {
      min-height: 40px;
      padding: 0 14px;
      border: 0;
      border-radius: 999px;
      background: var(--papel);
      color: var(--tinta);
      font: inherit;
      font-weight: 800;
      font-size: 0.875rem;
      cursor: pointer;
    }

    .casar__acciones {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 8px;
    }

    .casar__accion {
      min-height: 40px;
      padding: 0 10px;
      border: 0;
      background: none;
      color: var(--tinta);
      font: inherit;
      font-weight: 800;
      font-size: 0.8125rem;
      text-decoration: underline;
      text-decoration-color: var(--rosa-boton);
      text-decoration-thickness: 2px;
      text-underline-offset: 3px;
      cursor: pointer;
    }

    @keyframes abre {
      from {
        opacity: 0;
        transform: translateY(-4px);
      }
    }

    .clasificar__nota {
      margin: 0;
      color: var(--tinta-suave);
      font-size: 0.8125rem;
    }

    .clasificar__tamano {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 800;
      font-size: 0.9375rem;
    }

    .clasificar__cifra {
      width: 6.5ch;
      min-height: 44px;
      padding: 0 10px;
      text-align: center;
    }

    .casar__opcion--elegida {
      background: var(--placa-precio);
      box-shadow: 0 0 0 2px var(--tinta);
    }

    .clasificar__guardar {
      min-height: 40px;
      font-size: 0.875rem;
    }
  `,
})
export class ClasificarLinea {
  /** Para ids únicos en la página. */
  readonly clave = input.required<string>();
  readonly texto = input.required<string>();
  readonly unidadLinea = input.required<Unidad>();
  /** Null mientras se carga. */
  readonly catalogo = input.required<readonly GenericoResumen[] | null>();
  readonly actual = input<{ genericoId: string | null; tamano: number | null } | null>(null);
  readonly ocupado = input(false);
  readonly error = input<string | null>(null);

  readonly elegir$ = output<Eleccion>({ alias: 'guardar' });
  readonly ninguno = output<void>();

  protected readonly nombreCategoria = NOMBRE_CATEGORIA;
  protected readonly porUnidad = porUnidad;
  protected readonly busqueda = signal('');
  /** Undefined: lo que ya tenía la línea. Null: nada elegido. */
  private readonly elegidoId = signal<string | null | undefined>(undefined);
  protected readonly tamanoTexto = signal('');

  protected readonly elegido = computed(() => {
    const elegido = this.elegidoId();
    const id = elegido === undefined ? (this.actual()?.genericoId ?? null) : elegido;
    return this.catalogo()?.find((g) => g.id === id) ?? null;
  });

  /** Lo que coincide con lo escrito; sin escribir, nada (son casi cien). */
  protected readonly opciones = computed(() => {
    const catalogo = this.catalogo() ?? [];
    const q = normalizar(this.busqueda());
    if (!q) {
      const g = this.elegido();
      return g ? [g] : [];
    }
    return catalogo.filter((g) => normalizar(g.nombre).includes(q)).slice(0, 8);
  });

  constructor() {
    queueMicrotask(() => {
      const t = this.actual()?.tamano;
      if (t != null) this.tamanoTexto.set(t.toLocaleString('es-ES'));
    });
  }

  protected elegir(g: GenericoResumen): void {
    this.elegidoId.set(g.id);
    this.busqueda.set('');
  }

  protected quitarEleccion(): void {
    this.elegidoId.set(null);
    this.busqueda.set('');
  }

  protected guardar(): void {
    const g = this.elegido();
    if (!g) return;
    const tamano = Number(this.tamanoTexto().replace(',', '.'));
    this.elegir$.emit({ genericoId: g.id, tamano: this.tamanoTexto().trim() && tamano > 0 ? tamano : null });
  }

  protected medida(unidad: Unidad): string {
    return unidad === 'KG' ? 'kg' : unidad === 'L' ? 'L' : 'ud';
  }

  protected valor(evento: Event): string {
    return (evento.target as HTMLInputElement).value;
  }
}

function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}
