import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ChartConfiguration } from 'chart.js';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { RepostajesService } from '../core/repostajes.service';
import { Vehiculo } from '../core/repostajes.model';
import { Grafica } from './grafica';

/**
 * Las gráficas de un vehículo.
 *
 * Tres, y cada una responde a una pregunta distinta:
 *   · ¿cuánto consume el coche, y está empeorando?
 *   · ¿cuánto me gasto al mes?
 *   · ¿estoy repostando barato o caro?
 *
 * Cuando no hay datos suficientes, cada una explica QUÉ falta para que aparezca, en vez
 * de enseñar un hueco vacío: son gráficas que se llenan con el tiempo, y quien acaba de
 * empezar merece saber cuánto le queda.
 */
@Component({
  selector: 'app-graficas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Grafica],
  template: `
    @if (vehiculos().length > 1) {
      <div class="selector-vehiculo">
        <label for="grafica-vehiculo">Vehículo</label>
        <select id="grafica-vehiculo" (change)="elegirVehiculo($any($event.target).value)">
          @for (vehiculo of vehiculos(); track vehiculo.id) {
            <option [value]="vehiculo.id" [selected]="vehiculo.id === vehiculoId()">
              {{ vehiculo.nombre }}
            </option>
          }
        </select>
      </div>
    }

    @if (estadisticas.isLoading()) {
      <p class="estado" role="status">Calculando…</p>
    } @else if (estadisticas.value(); as datos) {
      <!-- Cifras de cabecera: lo que se mira antes que ninguna gráfica. -->
      <dl class="resumen">
        <div>
          <dt>Consumo medio</dt>
          <dd>
            @if (datos.consumoMedio !== null) {
              {{ datos.consumoMedio.toFixed(1) }} <span class="unidad">l/100km</span>
            } @else {
              <span class="sin-dato">—</span>
            }
          </dd>
        </div>
        <div>
          <dt>Gastado</dt>
          <dd>{{ datos.gastoTotal.toFixed(2) }} <span class="unidad">€</span></dd>
        </div>
        <div>
          <dt>Recorridos</dt>
          <dd>{{ datos.kilometrosRecorridos.toLocaleString('es-ES') }} <span class="unidad">km</span></dd>
        </div>
        <div>
          <dt>{{ datos.ahorroFrenteALaMedia >= 0 ? 'Ahorrado' : 'De más' }}</dt>
          <dd [class.bueno]="datos.ahorroFrenteALaMedia > 0">
            {{ (datos.ahorroFrenteALaMedia >= 0 ? datos.ahorroFrenteALaMedia : -datos.ahorroFrenteALaMedia).toFixed(2) }}
            <span class="unidad">€</span>
          </dd>
        </div>
        <!--
          El de al lado dice cómo lo has hecho; este dice cuánto te queda por hacer. Solo
          aparece si hay algo que decir: en un historial impecable, o en uno anterior a que
          se guardara la zona, un "0,00 €" no informa de nada.
        -->
        @if (datos.dejadoEnLaMesa > 0) {
          <div>
            <dt>Podrías haber ahorrado</dt>
            <dd>{{ datos.dejadoEnLaMesa.toFixed(2) }} <span class="unidad">€</span></dd>
          </div>
        }
      </dl>

      <section class="grafica-caja">
        <h3>Consumo</h3>
        @if (consumo(); as config) {
          <div class="lienzo">
            <app-grafica [configuracion]="config" [descripcion]="descripcionConsumo()" />
          </div>
        } @else {
          <p class="pendiente">
            Aparece con dos repostajes a depósito lleno y los kilómetros apuntados. Llevas
            {{ datos.consumos.length }} de 1.
          </p>
        }
      </section>

      <section class="grafica-caja">
        <h3>Gasto por mes</h3>
        @if (gasto(); as config) {
          <div class="lienzo">
            <app-grafica [configuracion]="config" [descripcion]="descripcionGasto()" />
          </div>
        } @else {
          <p class="pendiente">Aparece en cuanto apuntes tu primer repostaje.</p>
        }
      </section>

      <section class="grafica-caja">
        <h3>Lo que pagas por litro</h3>
        @if (precios(); as config) {
          <div class="lienzo">
            <app-grafica [configuracion]="config" [descripcion]="descripcionPrecios()" />
          </div>
          <p class="nota">
            La línea de la media provincial solo aparece en los repostajes en los que
            elegiste gasolinera.
          </p>
        } @else {
          <p class="pendiente">Aparece con dos repostajes o más.</p>
        }
      </section>
    }
  `,
  styleUrl: './graficas.scss',
})
export class Graficas {
  private readonly servicio = inject(RepostajesService);

  readonly vehiculos = input.required<Vehiculo[]>();

  private readonly elegido = signal<string | null>(null);

  /** El elegido a mano o, por defecto, el primero. */
  protected readonly vehiculoId = computed(
    () => this.elegido() ?? this.vehiculos()[0]?.id ?? null,
  );

  protected readonly estadisticas = rxResource({
    params: () => ({ id: this.vehiculoId() }),
    stream: ({ params }) =>
      params.id
        ? this.servicio.estadisticas(params.id).pipe(catchError(() => of(null)))
        : of(null),
  });

  protected elegirVehiculo(id: string): void {
    this.elegido.set(id);
  }

  // ── Configuraciones de las gráficas ── //

  protected readonly consumo = computed<ChartConfiguration | null>(() => {
    const tramos = this.estadisticas.value()?.consumos ?? [];
    if (tramos.length === 0) {
      return null;
    }

    return {
      type: 'line',
      data: {
        labels: tramos.map((t) => fechaCorta(t.hasta)),
        datasets: [{ label: 'l/100 km', data: tramos.map((t) => t.consumo), fill: true }],
      },
      options: {
        // El consumo NO empieza en cero a propósito: entre 6,2 y 6,8 hay una diferencia
        // real que un eje desde cero aplasta hasta hacerla invisible.
        scales: { y: { beginAtZero: false } },
      },
    };
  });

  protected readonly gasto = computed<ChartConfiguration | null>(() => {
    const meses = this.estadisticas.value()?.gastoMensual ?? [];
    if (meses.length === 0) {
      return null;
    }

    return {
      type: 'bar',
      data: {
        labels: meses.map((m) => mesCorto(m.mes)),
        datasets: [{ label: '€', data: meses.map((m) => m.importe) }],
      },
      // El dinero SÍ empieza en cero: son cantidades absolutas y su tamaño relativo es
      // justo lo que se compara de un mes a otro.
      options: { scales: { y: { beginAtZero: true } } },
    };
  });

  protected readonly precios = computed<ChartConfiguration | null>(() => {
    const precios = this.estadisticas.value()?.precios ?? [];
    if (precios.length < 2) {
      return null;
    }

    return {
      type: 'line',
      data: {
        labels: precios.map((p) => fechaCorta(p.fecha)),
        datasets: [
          { label: 'Lo que pagaste', data: precios.map((p) => p.precioLitro), fill: true },
          {
            // "De referencia" y no "de la zona" a propósito: en un historial largo esta
            // línea mezcla medias de zona (las nuevas) con medias provinciales (las
            // antiguas, que ya no se pueden recalcular). Ponerle el nombre de una de las
            // dos sería mentir en la mitad de los puntos.
            label: 'Media de referencia',
            data: precios.map((p) => p.precioReferencia),
            fill: false,
            borderDash: [5, 4],
            pointRadius: 0,
            // Une los puntos aunque falte alguno: los repostajes sin gasolinera elegida
            // no tienen media, y cortar la línea ahí sugeriría que el precio se desplomó.
            spanGaps: true,
          },
        ],
      },
      options: { scales: { y: { beginAtZero: false } } },
    };
  });

  // ── Descripciones para lectores de pantalla ── //

  protected readonly descripcionConsumo = computed(() => {
    const tramos = this.estadisticas.value()?.consumos ?? [];
    const medio = this.estadisticas.value()?.consumoMedio;
    return `Consumo por tramo, ${tramos.length} medidas. Media: ${medio ?? '—'} litros a los 100 km.`;
  });

  protected readonly descripcionGasto = computed(() => {
    const total = this.estadisticas.value()?.gastoTotal ?? 0;
    const meses = this.estadisticas.value()?.gastoMensual.length ?? 0;
    return `Gasto mensual en combustible durante ${meses} meses. Total: ${total.toFixed(2)} euros.`;
  });

  protected readonly descripcionPrecios = computed(() => {
    const precios = this.estadisticas.value()?.precios ?? [];
    return `Precio por litro pagado en ${precios.length} repostajes, comparado con la media de la provincia.`;
  });
}

/** "2026-03-08" → "8 mar" */
function fechaCorta(iso: string): string {
  return new Date(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
}

/** "2026-03" → "mar 26" */
function mesCorto(mes: string): string {
  const [anyo, numero] = mes.split('-');
  const fecha = new Date(Number(anyo), Number(numero) - 1, 1);
  return `${fecha.toLocaleDateString('es-ES', { month: 'short' })} ${anyo.slice(2)}`;
}
