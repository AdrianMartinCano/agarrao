import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '@adrianmartincano/ng-auth';
import { IonContent, IonFooter } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { Pestanas } from '../pestanas/pestanas';
import { EstacionesService } from './core/estaciones.service';

/**
 * La parte de Gasolina (lo que era Gasofa): el armazón con sus pestañas de
 * arriba y, dentro, el buscador, los viajes, las favoritas, los repostajes y los vehículos.
 *
 * Buscar gasolineras y Viajes son públicos: sin sesión se ven solo esos dos y, en vez de la barra
 * de abajo, un «Entrar» para el resto.
 */
@Component({
  selector: 'app-gasolina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterOutlet, RouterLink, RouterLinkActive, Pestanas],
  template: `
    <ion-content class="trama">
      <main class="gasolina armazon">
        <header class="armazon__cabeza">
          <h1 class="titular">Gasolina</h1>
          <nav class="secciones" aria-label="Gasolina">
            <a
              class="seccion"
              routerLink="/gasolina"
              routerLinkActive="seccion--aqui"
              [routerLinkActiveOptions]="{ exact: true }"
              >Buscar</a
            >
            <a class="seccion" routerLink="/gasolina/viajes" routerLinkActive="seccion--aqui">Viajes</a>
            @if (conSesion()) {
              <a class="seccion" routerLink="/gasolina/favoritas" routerLinkActive="seccion--aqui">Favoritas</a>
              <a class="seccion" routerLink="/gasolina/repostajes" routerLinkActive="seccion--aqui">Repostajes</a>
              <a class="seccion" routerLink="/gasolina/vehiculos" routerLinkActive="seccion--aqui">Coches</a>
            }
          </nav>
        </header>

        <router-outlet />

        <footer class="armazon__pie">
          @if (datos(); as d) {
            <p><strong>{{ d.estaciones }}</strong> gasolineras · precios {{ d.hace }}</p>
          }
          <p>
            Datos del
            <a href="https://geoportalgasolineras.es/" target="_blank" rel="noopener noreferrer"
              >Ministerio para la Transición Ecológica</a
            >, que los publica cada media hora.
          </p>
        </footer>
      </main>
    </ion-content>

    <ion-footer class="ion-no-border pie">
      @if (conSesion()) {
        <app-pestanas activa="gasolina" />
      } @else {
        <div class="sin-sesion">
          <a class="boton-rosa" routerLink="/">Entrar para guardar repostajes</a>
        </div>
      }
    </ion-footer>
  `,
  styles: `
    .armazon {
      display: grid;
      align-content: start;
      gap: 16px;
      max-width: 960px;
      margin-inline: auto;
      padding: max(18px, env(safe-area-inset-top)) 16px 24px;
    }

    .armazon__cabeza {
      display: grid;
      gap: 10px;
    }

    /* Las secciones: un carril lila con la elegida en tinta (como «Ordenar por») */
    .secciones {
      display: flex;
      justify-self: start;
      max-width: 100%;
      gap: 2px;
      padding: 3px;
      border-radius: 999px;
      background: var(--placa-nombre-lila);
      overflow-x: auto;
      scrollbar-width: none;
    }

    .seccion {
      flex: none;
      display: grid;
      place-items: center;
      min-height: 38px;
      padding: 0 16px;
      border-radius: 999px;
      color: var(--tinta);
      font-weight: 800;
      font-size: 0.9375rem;
      text-decoration: none;
      transition: background 200ms ease, color 200ms ease;
    }

    .seccion--aqui {
      background: var(--tinta);
      color: #fff;
    }

    .armazon__pie {
      display: grid;
      gap: 2px;
      margin-top: 8px;
      color: var(--tinta-suave);
      font-size: 0.8125rem;
    }

    .armazon__pie p {
      margin: 0;
    }

    .armazon__pie a {
      color: var(--tinta);
    }

    .sin-sesion {
      max-width: 528px;
      margin-inline: auto;
      padding: 0 16px max(12px, env(safe-area-inset-bottom));
    }
  `,
})
export class GasolinaPage {
  private readonly auth = inject(AuthService);

  protected readonly conSesion = computed(() => !!this.auth.currentUser());

  private readonly estado = toSignal(
    inject(EstacionesService)
      .estado()
      .pipe(catchError(() => of(null))),
    { initialValue: null },
  );

  /** Cuándo se bajaron los precios: caducan cada media hora y el Ministerio a veces cae. */
  protected readonly datos = computed(() => {
    const e = this.estado();
    if (!e?.disponible || !e.ultimaActualizacion) return null;
    return { estaciones: e.estaciones.toLocaleString('es-ES'), hace: hace(new Date(e.ultimaActualizacion)) };
  });
}

/** «hace 12 minutos», «hace 2 horas». */
function hace(desde: Date): string {
  const minutos = Math.round((Date.now() - desde.getTime()) / 60_000);
  if (minutos < 1) return 'de hace menos de un minuto';
  const formato = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });
  if (minutos < 60) return 'de ' + formato.format(-minutos, 'minute');
  const horas = Math.round(minutos / 60);
  return 'de ' + (horas < 24 ? formato.format(-horas, 'hour') : formato.format(-Math.round(horas / 24), 'day'));
}
