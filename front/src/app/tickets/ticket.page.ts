import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { ClasificacionLinea, GenericoResumen, precioPorUnidad } from '../precios/generico.model';
import { GenericosService } from '../precios/genericos.service';
import { ClasificarLinea, Eleccion } from './clasificar-linea';
import { LineaTicket, NOMBRE_CADENA, Ticket } from './ticket.model';
import { TicketsService } from './tickets.service';

/** Un ticket guardado: la tira de solo lectura, con lo que se casó cada línea. */
@Component({
  selector: 'app-ticket',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink, ClasificarLinea],
  template: `
    <ion-content class="trama">
      <main class="detalle">
        <a class="detalle__volver" routerLink="/tickets">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5l-7 7 7 7" /></svg>
          Tickets
        </a>

        @if (ticket(); as t) {
          <h1 class="sr-solo">Ticket de {{ nombreCadena[t.cadena] }} del {{ fecha(t.fecha) }}</h1>
          <article class="tira">
            <p class="tira__tienda">{{ nombreCadena[t.cadena] }}</p>
            <p class="tira__fecha">{{ fecha(t.fecha) }}{{ t.origen === 'FOTO' ? ' · desde una foto' : '' }}</p>
            <ul class="tira__lineas">
              @for (l of t.lineas; track l.id) {
                <li class="linea">
                  <span class="linea__texto">{{ l.texto }}</span>
                  <span class="linea__importe">{{ euros(l.importe) }}</span>
                  <div class="linea__pie">
                    @if (detalle(l); as d) {
                      <span class="linea__detalle">{{ d }}</span>
                    }
                    @if (l.nombre) {
                      <div class="linea__casado">
                        <span class="mini-ficha"><span class="mini-ficha__nombre">{{ l.nombre }}</span></span>
                      </div>
                    }
                  </div>
                  @if (l.importe > 0) {
                    @let c = clasificacion().get(l.id);
                    <!-- El genérico y su precio por unidad: menta, porque es un precio (Placa Fija) -->
                    <button
                      type="button"
                      class="linea__generico"
                      [class.linea__generico--sin]="!c?.genericoId"
                      [attr.aria-expanded]="abierta() === l.id"
                      (click)="abrir(l.id)"
                    >
                      @if (c?.genericoId) {
                        <span class="linea__generico-nombre">{{ c!.generico }}</span>
                        @if (c!.precioUnidad !== null && c!.unidad) {
                          <span class="linea__generico-precio">{{ precio(c!.precioUnidad!, c!.unidad!) }}</span>
                        } @else {
                          <span class="linea__generico-precio">¿cuánto trae?</span>
                        }
                      } @else {
                        <span class="linea__generico-nombre">{{ c ? 'Sin genérico' : '¿Qué genérico es?' }}</span>
                      }
                    </button>
                    @if (abierta() === l.id) {
                      <app-clasificar-linea
                        [clave]="l.id"
                        [texto]="l.texto"
                        [unidadLinea]="l.unidad"
                        [catalogo]="catalogo()"
                        [actual]="c ? { genericoId: c.genericoId, tamano: c.tamano } : null"
                        [ocupado]="guardando()"
                        [error]="errorClasificar()"
                        (guardar)="clasificar(t, l.texto, $event)"
                        (ninguno)="clasificar(t, l.texto, { genericoId: null, tamano: null })"
                      />
                    }
                  }
                </li>
              }
            </ul>
            <p class="tira__total"><span>TOTAL</span><span>{{ euros(t.total) }} €</span></p>
          </article>

          <button
            type="button"
            class="boton-papel detalle__borrar"
            [class.detalle__borrar--seguro]="confirmando()"
            (click)="borrar(t)"
          >
            {{ confirmando() ? '¿Seguro? Toca otra vez' : 'Borrar el ticket' }}
          </button>
        } @else if (!error()) {
          <p class="detalle__cargando" role="status">Cargando…</p>
        }

        @if (error()) {
          <p class="aviso-error" role="alert">{{ error() }}</p>
        }
      </main>
    </ion-content>
  `,
  styleUrls: ['./tira.scss'],
  styles: `
    .detalle {
      display: grid;
      align-content: start;
      gap: 18px;
      max-width: 480px;
      margin-inline: auto;
      padding: max(14px, env(safe-area-inset-top)) 16px 32px;
    }

    .detalle__volver {
      justify-self: start;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-height: 44px;
      padding-right: 10px;
      font-weight: 800;
      text-decoration: none;
    }

    .detalle__volver svg {
      width: 22px;
      height: 22px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.6;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    .detalle__borrar { justify-self: center; }

    .linea__generico {
      grid-column: 1 / -1;
      justify-self: end;
      display: inline-flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 0 8px;
      max-width: 100%;
      min-height: 34px;
      margin-top: 4px;
      padding: 4px 12px;
      border: 0;
      border-radius: 999px;
      background: var(--placa-precio);
      color: var(--tinta);
      font: inherit;
      font-weight: 800;
      font-size: 0.8125rem;
      text-align: right;
      cursor: pointer;
      transition: transform 300ms var(--salida);
    }

    .linea__generico:active { transform: scale(0.97); }

    .linea__generico-precio { font-weight: 500; }

    /* Sin genérico: una invitación discreta, no un precio */
    .linea__generico--sin {
      background: none;
      color: var(--tinta-suave);
      text-decoration: underline;
      text-decoration-color: var(--rosa-boton);
      text-decoration-thickness: 2px;
      text-underline-offset: 3px;
    }

    .detalle__borrar--seguro {
      border-color: var(--rosa-boton);
      background: var(--rosa-boton);
      color: var(--papel);
    }

    .detalle__cargando {
      margin: 24px 0 0;
      text-align: center;
      font-weight: 800;
    }
  `,
})
export class TicketPage implements OnInit {
  private readonly tickets = inject(TicketsService);
  private readonly hogares = inject(HogaresService);
  private readonly genericos = inject(GenericosService);
  private readonly router = inject(Router);

  /** De la ruta (`/tickets/:id`). */
  readonly id = input.required<string>();

  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly ticket = signal<Ticket | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmando = signal(false);

  private readonly clasificaciones = signal<readonly ClasificacionLinea[]>([]);
  protected readonly clasificacion = computed(() => new Map(this.clasificaciones().map((c) => [c.lineaId, c])));
  /** La línea con el panel de genérico abierto. */
  protected readonly abierta = signal<string | null>(null);
  /** Se pide la primera vez que se abre un panel. */
  protected readonly catalogo = signal<readonly GenericoResumen[] | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorClasificar = signal<string | null>(null);

  private readonly hogarId = this.hogares.activo()?.id ?? '';

  ngOnInit(): void {
    this.tickets.detalle(this.hogarId, this.id()).subscribe({
      next: (t) => this.ticket.set(t),
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha podido cargar el ticket.')),
    });
    this.cargarClasificacion();
  }

  protected abrir(lineaId: string): void {
    this.errorClasificar.set(null);
    this.abierta.update((a) => (a === lineaId ? null : lineaId));
    if (this.catalogo() === null) {
      this.genericos.catalogo(this.hogarId).subscribe({
        next: (c) => this.catalogo.set(c),
        error: (e: unknown) => this.errorClasificar.set(mensajeDeError(e)),
      });
    }
  }

  /** Corrige el producto entero (su texto en esta cadena), no solo esta línea. */
  protected clasificar(t: Ticket, texto: string, eleccion: Eleccion): void {
    this.guardando.set(true);
    this.genericos.clasificar(this.hogarId, { cadena: t.cadena, texto, ...eleccion }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.abierta.set(null);
        this.cargarClasificacion();
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errorClasificar.set(mensajeDeError(e));
      },
    });
  }

  private cargarClasificacion(): void {
    this.genericos.deTicket(this.hogarId, this.id()).subscribe({
      next: (c) => this.clasificaciones.set(c),
      error: () => undefined, // Sin genéricos el ticket se lee igual.
    });
  }

  protected precio(valor: number, unidad: LineaTicket['unidad']): string {
    return precioPorUnidad(valor, unidad);
  }

  protected borrar(t: Ticket): void {
    if (!this.confirmando()) {
      this.confirmando.set(true);
      return;
    }
    this.tickets.borrar(this.hogarId, t.id).subscribe({
      next: () => void this.router.navigateByUrl('/tickets', { replaceUrl: true }),
      error: (e: unknown) => {
        this.confirmando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected fecha(iso: string): string {
    return new Date(`${iso}T12:00:00`).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  protected detalle(l: LineaTicket): string | null {
    if (l.unidad !== 'UD') {
      const u = l.unidad === 'KG' ? 'kg' : 'L';
      return `${l.cantidad.toLocaleString('es-ES')} ${u}${l.precioUnitario ? ` × ${this.euros(l.precioUnitario)} €/${u}` : ''}`;
    }
    return l.cantidad > 1 && l.precioUnitario ? `${l.cantidad} × ${this.euros(l.precioUnitario)}` : null;
  }
}
