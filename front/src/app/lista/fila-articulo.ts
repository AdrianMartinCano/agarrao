import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';

import { inicial } from '../hogar/cintas';
import { Articulo } from './articulo.model';

/**
 * Una cosa de la lista: fila papel con el banderín de quien la apuntó.
 *
 * Tocar la fila la tacha (o la destacha en el carro). El botón de la derecha abre la
 * fila para cambiar la cantidad o quitarla: lo que se hace con el pulgar en el súper es
 * tachar, y eso no puede competir con nada.
 */
@Component({
  selector: 'app-fila-articulo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'fila',
    '[class.fila--comprada]': 'articulo().comprado',
    '[class.fila--recien]': 'recien()',
    '[class.fila--abierta]': 'abierta()',
    '[style.--cinta]': 'cinta()',
    // Nombre único por fila: al tachar, la View Transition la lleva de su sitio al carro.
    '[style.view-transition-name]': "'fila-' + articulo().id",
  },
  template: `
    @let a = articulo();
    <div class="fila__cuerpo">
      <button
        type="button"
        class="fila__toque"
        [attr.aria-pressed]="a.comprado"
        [attr.aria-label]="(a.comprado ? 'Sacar del carro: ' : 'Al carro: ') + a.nombre + (a.cantidad > 1 ? ', ' + a.cantidad : '')"
        (click)="marcar.emit(!a.comprado)"
      >
        <span class="fila__cinta" aria-hidden="true">{{ letra() }}</span>
        <span class="fila__nombre">{{ a.nombre }}</span>
        @if (a.cantidad > 1) {
          <span class="fila__cantidad" aria-hidden="true">×{{ a.cantidad }}</span>
        }
      </button>
      @if (!a.comprado) {
        <button
          type="button"
          class="fila__mas"
          [attr.aria-expanded]="abierta()"
          [attr.aria-label]="'Cantidad o quitar: ' + a.nombre"
          (click)="abierta.set(!abierta())"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="5" cy="12" r="1.6" />
            <circle cx="12" cy="12" r="1.6" />
            <circle cx="19" cy="12" r="1.6" />
          </svg>
        </button>
      }
    </div>
    @if (abierta() && !a.comprado) {
      <div class="fila__ajustes">
        <div class="paso" role="group" [attr.aria-label]="'Cantidad de ' + a.nombre">
          <button type="button" class="paso__boton" aria-label="Uno menos" [disabled]="a.cantidad <= 1" (click)="cantidad.emit(a.cantidad - 1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12h12" /></svg>
          </button>
          <span class="paso__valor" aria-live="polite">{{ a.cantidad }}</span>
          <button type="button" class="paso__boton" aria-label="Uno más" [disabled]="a.cantidad >= 99" (click)="cantidad.emit(a.cantidad + 1)">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 12h12M12 6v12" /></svg>
          </button>
        </div>
        @if (a.apuntadoPor) {
          <span class="fila__quien">Lo apuntó {{ a.apuntadoPor }}</span>
        }
        <button type="button" class="fila__quitar" (click)="quitar.emit()">Quitar</button>
      </div>
    }
  `,
  styleUrl: './fila-articulo.scss',
})
export class FilaArticulo {
  readonly articulo = input.required<Articulo>();
  /** Color de cinta de quien lo apuntó. */
  readonly cinta = input.required<string>();
  /** Recién tachado en esta pantalla: el trazo se dibuja (si no, ya está puesto). */
  readonly recien = input(false);

  readonly marcar = output<boolean>();
  readonly cantidad = output<number>();
  readonly quitar = output<void>();

  protected readonly abierta = signal(false);
  protected readonly letra = computed(() => inicial(this.articulo().apuntadoPor));
}
