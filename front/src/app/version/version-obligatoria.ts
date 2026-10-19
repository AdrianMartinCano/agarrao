import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { VersionService } from '../core/version.service';

/**
 * La pantalla que bloquea cuando esta versión ya no vale con el servidor. Tapa TODO,
 * también la de entrar: con una versión vieja, hasta el login puede fallar de formas
 * raras, y es mejor una sola salida clara.
 *
 * Listillo sujeta la tarjeta como un cartel: el dibujo acaba en sus manos, y las manos
 * quedan encima del borde rosa.
 */
@Component({
  selector: 'app-version-obligatoria',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let c = version.comprobacion();
    <main class="bloqueo trama" aria-labelledby="bloqueo-titulo">
      <div class="bloqueo__conjunto">
        <img class="bloqueo__personaje" src="personajes/listillo-cartel.svg" alt="" width="240" height="230" />
        <div class="bloqueo__cartel">
          <h1 id="bloqueo-titulo" class="bloqueo__titulo">Toca actualizar</h1>
          <p class="bloqueo__texto">
            Tu Agarrao ({{ version.actual }}) se ha quedado viejo y ya no se entiende con el
            servidor. La versión {{ c?.ultima }} está lista: es un momento.
          </p>
          @if (c?.notas) {
            <p class="bloqueo__notas">{{ c!.notas }}</p>
          }
          <button type="button" class="bloqueo__boton" (click)="actualizar()" [disabled]="actualizando()">
            {{ actualizando() ? 'Actualizando…' : 'Actualizar ahora' }}
          </button>
        </div>
      </div>
    </main>
  `,
  styles: `
    :host {
      position: fixed;
      inset: 0;
      z-index: 30;
      display: block;
      overflow-y: auto;
    }

    .bloqueo {
      display: grid;
      place-items: center;
      min-height: 100%;
      padding: 24px 18px;
    }

    .bloqueo__conjunto {
      display: grid;
      justify-items: center;
      max-width: 400px;
      width: 100%;
      animation: rebote 900ms var(--salida) both;
    }

    /* Las manos (abajo del dibujo) caen sobre el borde de 4 px del cartel */
    .bloqueo__personaje {
      position: relative;
      z-index: 1;
      width: 240px;
      height: auto;
      margin-bottom: -18px;
    }

    .bloqueo__cartel {
      display: grid;
      justify-items: center;
      gap: 12px;
      width: 100%;
      padding: 30px 22px 24px;
      border-radius: 32px;
      background: var(--papel);
      border: 4px solid var(--rosa-boton);
      box-shadow: var(--sombra-ficha);
      text-align: center;
    }

    .bloqueo__titulo {
      margin: 0;
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 2.5rem;
      line-height: 1.05;
    }

    .bloqueo__texto {
      margin: 0;
      line-height: 1.5;
      text-wrap: pretty;
    }

    .bloqueo__notas {
      margin: 0;
      padding: 8px 14px;
      border-radius: 14px;
      background: var(--placa-nota);
      font-size: 0.875rem;
    }

    .bloqueo__boton {
      width: 100%;
      min-height: 58px;
      margin-top: 6px;
      border: 0;
      border-radius: 999px;
      background: var(--rosa-boton);
      color: #fff;
      font: inherit;
      font-size: 1.125rem;
      font-weight: 800;
      box-shadow: var(--sombra-boton);
      cursor: pointer;
    }

    .bloqueo__boton:disabled { opacity: 0.75; cursor: progress; }

    @keyframes rebote {
      from { transform: translateY(-30px) scale(0.92); opacity: 0; }
    }
  `,
})
export class VersionObligatoria {
  protected readonly version = inject(VersionService);
  protected readonly actualizando = signal(false);

  protected actualizar(): void {
    this.actualizando.set(true);
    void this.version.actualizar();
  }
}
