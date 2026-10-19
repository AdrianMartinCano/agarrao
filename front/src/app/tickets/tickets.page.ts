import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { NOMBRE_CADENA, TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';

/** Los tickets del hogar, del último al primero, cada uno como un trozo de tira. */
@Component({
  selector: 'app-tickets',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  template: `
    <ion-content class="trama">
      <main class="tickets">
        <header class="tickets__cabeza">
          <a class="tickets__volver" routerLink="/lista">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5l-7 7 7 7" /></svg>
            La lista
          </a>
          <h1 class="titular">Tickets</h1>
        </header>

        <a class="boton-rosa tickets__subir" routerLink="/tickets/nuevo">Subir un ticket</a>

        @if (error()) {
          <p class="aviso-error" role="alert">{{ error() }}</p>
        }

        @if (lista(); as ts) {
          @if (ts.length === 0) {
            <section class="vacio">
              <img src="personajes/listillo.svg" alt="" width="120" height="120" />
              <p class="vacio__texto">
                Aún no hay tickets. Sube el de tu próxima compra y Agarrao aprenderá qué compras y dónde.
              </p>
            </section>
          } @else {
            <ul class="tickets__lista">
              @for (t of ts; track t.id) {
                <li>
                  <a class="resguardo" [routerLink]="['/tickets', t.id]">
                    <span class="resguardo__tienda">{{ nombreCadena[t.cadena] }}</span>
                    <span class="resguardo__total">{{ euros(t.total) }} €</span>
                    <span class="resguardo__fecha">{{ fecha(t.fecha) }}</span>
                    <span class="resguardo__lineas">
                      {{ t.lineas }} líneas@if (t.casadas < t.lineas) { · {{ t.lineas - t.casadas }} sin casar}
                    </span>
                  </a>
                </li>
              }
            </ul>
          }
        } @else if (!error()) {
          <p class="tickets__cargando" role="status">Cargando…</p>
        }
      </main>
    </ion-content>
  `,
  styleUrl: './tickets.page.scss',
})
export class TicketsPage {
  private readonly tickets = inject(TicketsService);
  private readonly hogares = inject(HogaresService);

  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly lista = signal<TicketResumen[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const hogar = this.hogares.activo();
    if (!hogar) return;
    this.tickets.listar(hogar.id).subscribe({
      next: (ts) => this.lista.set(ts),
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se han podido cargar los tickets.')),
    });
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected fecha(iso: string): string {
    return new Date(`${iso}T12:00:00`).toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric', month: 'short' });
  }
}
