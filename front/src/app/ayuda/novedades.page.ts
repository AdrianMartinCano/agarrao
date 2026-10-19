import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { NOVEDADES } from '../core/novedades';
import { VERSION_APP } from '../core/version-app';

/** Lo que ha cambiado en cada versión (sale de CHANGELOG.md, ver tools/escribir-version.mjs). */
@Component({
  selector: 'app-novedades',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  template: `
    <ion-content class="trama">
      <main class="novedades">
        <header class="novedades__cabeza">
          <a class="novedades__volver" routerLink="/hogar">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5l-7 7 7 7" /></svg>
            Hogar
          </a>
          <h1 class="titular">Novedades</h1>
          <p class="novedades__nota">Tienes la versión {{ version }}.</p>
        </header>

        @for (n of novedades; track n.version; let primera = $first) {
          <section class="version" [class.version--actual]="primera" [attr.aria-labelledby]="'v-' + n.version">
            <h2 class="version__titulo" [id]="'v-' + n.version">
              <span class="version__numero">{{ n.version }}</span>
              {{ n.titulo ?? '' }}
              <span class="version__fecha">{{ fecha(n.fecha) }}</span>
            </h2>
            <ul class="version__cambios">
              @for (c of n.cambios; track c) {
                <li>{{ c }}</li>
              }
            </ul>
          </section>
        }
      </main>
    </ion-content>
  `,
  styles: `
    .novedades {
      display: grid;
      align-content: start;
      gap: 18px;
      max-width: 528px;
      margin-inline: auto;
      padding: max(18px, env(safe-area-inset-top)) 16px max(28px, env(safe-area-inset-bottom));
    }

    .novedades__cabeza {
      display: grid;
      gap: 4px;
    }

    .novedades__volver {
      justify-self: start;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      min-height: 44px;
      padding-right: 10px;
      color: var(--tinta);
      font-weight: 800;
      text-decoration: none;
    }

    .novedades__volver svg {
      width: 22px;
      height: 22px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.8;
      stroke-linecap: round;
      stroke-linejoin: round;
    }

    .novedades__nota {
      margin: 0;
      color: var(--tinta-suave);
    }

    .version {
      display: grid;
      gap: 8px;
      padding: 16px;
      border: 3px solid var(--papel);
      border-radius: var(--radio-ficha);
      background: var(--papel);
      box-shadow: var(--sombra-ficha);
    }

    /* La última, en lila y algo inclinada: es la que acaba de llegar */
    .version--actual {
      background: var(--lila);
      transform: rotate(-0.8deg);
    }

    .version__titulo {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 4px 10px;
      margin: 0;
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1.375rem;
      line-height: 1.15;
    }

    .version__numero {
      padding: 2px 10px;
      border-radius: 999px;
      background: var(--placa-precio);
      font-family: var(--letra-cuerpo);
      font-size: 0.9375rem;
      font-variant-numeric: tabular-nums;
    }

    .version__fecha {
      color: var(--tinta-suave);
      font-family: var(--letra-cuerpo);
      font-weight: 700;
      font-size: 0.875rem;
    }

    .version__cambios {
      display: grid;
      gap: 6px;
      margin: 0;
      padding-left: 20px;
    }

    .version__cambios li::marker {
      color: var(--rosa-boton);
    }
  `,
})
export class NovedadesPage {
  protected readonly novedades = NOVEDADES;
  protected readonly version = VERSION_APP;

  protected fecha(iso: string): string {
    return new Date(`${iso}T12:00:00`).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
  }
}
