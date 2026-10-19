import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent, IonFooter } from '@ionic/angular';

import { PartesService } from '../core/partes.service';
import { Pestanas } from '../pestanas/pestanas';

/**
 * El ＋ de la barra: un solo sitio para meter cosas. Subir un ticket, anotar un gasto o
 * apuntar algo en la lista (según las partes que use cada uno).
 */
@Component({
  selector: 'app-anadir',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterLink, Pestanas],
  template: `
    <ion-content class="trama">
      <main class="anadir">
        <header class="anadir__cabeza">
          <h1 class="titular">¿Qué apuntas?</h1>
          <p class="anadir__nota">Lo que metas aquí se cuenta solo en sus gráficas.</p>
        </header>

        <nav class="caminos" aria-label="Qué añadir">
          <a class="camino camino--ticket" routerLink="/tickets/nuevo">
            <span class="camino__cinta" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6M9 16h3" /></svg>
            </span>
            <span class="camino__nombre">Subir un ticket</span>
            <span class="camino__texto">
              El PDF de la app del súper o una foto. Si es del súper, sus precios van también a Precios.
            </span>
          </a>
          @if (partes.gastos()) {
            <a class="camino camino--gasto" routerLink="/gastos/nuevo">
              <span class="camino__cinta" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M4 20l4-1 11-11-3-3L5 16l-1 4zM14 6l3 3" /></svg>
              </span>
              <span class="camino__nombre">Anotar un gasto</span>
              <span class="camino__texto">
                La cena del sábado, la gasolina, Netflix… Lo que se repite cada mes se apunta una vez.
              </span>
            </a>
          }
          @if (partes.lista()) {
            <a class="camino camino--lista" routerLink="/lista">
              <span class="camino__cinta" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M9 6h11M9 12h11M9 18h11M4 6l.01 0M4 12l.01 0M4 18l.01 0" /></svg>
              </span>
              <span class="camino__nombre">Algo para la lista</span>
              <span class="camino__texto">Lo que hay que comprar, escrito o escaneando el código de barras.</span>
            </a>
          }
        </nav>
      </main>
    </ion-content>

    <ion-footer class="ion-no-border pie">
      <app-pestanas activa="anadir" />
    </ion-footer>
  `,
  styles: `
    .anadir {
      display: grid;
      align-content: start;
      gap: 22px;
      max-width: 528px;
      margin-inline: auto;
      padding: max(22px, env(safe-area-inset-top)) 16px 24px;
    }

    .anadir__cabeza {
      display: grid;
      gap: 4px;
    }

    .anadir__nota {
      margin: 0;
      color: var(--tinta-suave);
    }

    .caminos {
      display: grid;
      gap: 20px;
    }

    /* Fichas del catálogo, inclinadas a mano, con su cinta arriba (como Empezar) */
    .camino {
      position: relative;
      display: grid;
      gap: 6px;
      padding: 18px 18px 18px 64px;
      border: 3px solid var(--papel);
      border-radius: var(--radio-ficha);
      box-shadow: var(--sombra-ficha);
      color: var(--tinta);
      text-decoration: none;
      transform: rotate(-1.2deg);
      transition: transform 420ms var(--salida), box-shadow 420ms var(--salida);
    }

    .camino:nth-child(2) { transform: rotate(0.8deg); }

    .camino:hover,
    .camino:focus-visible {
      transform: rotate(0deg) translateY(-2px);
      box-shadow: 0 12px 24px -8px rgb(42 47 90 / 0.3), 0 2px 4px rgb(42 47 90 / 0.08);
    }

    .camino--ticket { --cinta: var(--cinta-leche); background: var(--cielo); }
    .camino--gasto { --cinta: var(--cinta-listillo); background: var(--rubor); }
    .camino--lista { --cinta: var(--cinta-lechuga); background: var(--menta); }

    .camino__cinta {
      position: absolute;
      top: -6px;
      left: 16px;
      display: grid;
      place-items: start center;
      width: 34px;
      height: 44px;
      padding-top: 7px;
      background: color-mix(in srgb, var(--cinta) 45%, var(--papel));
      clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 80%, 0 100%);
    }

    .camino__cinta svg {
      width: 20px;
      height: 20px;
      fill: none;
      stroke: var(--tinta);
      stroke-width: 2.2;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    .camino__nombre {
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1.5rem;
      line-height: 1.1;
    }

    .camino__texto {
      color: var(--tinta-suave);
      font-size: 0.9375rem;
      text-wrap: pretty;
    }
  `,
})
export class AnadirPage {
  protected readonly partes = inject(PartesService);
}
