import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { VersionService } from '../core/version.service';

/**
 * La cinta de «hay versión nueva»: una franja rosa arriba del todo, en el flujo de la
 * página (empuja la pantalla hacia abajo en vez de taparla). Opcional: se cierra con la X
 * y la app sigue funcionando. Vive en la raíz, así sale entres por donde entres.
 *
 * Las notas de la versión no van aquí: en una fila de 390 px no caben, y la cinta solo
 * tiene que decir «hay una nueva» y dejar actualizar con un toque.
 */
@Component({
  selector: 'app-aviso-version',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let c = version.comprobacion();
    @if (c?.estado === 'OPCIONAL') {
      <div class="cinta" role="status">
        <img class="cinta__personaje" src="personajes/listillo.svg" alt="" width="36" height="36" />
        <p class="cinta__texto">
          <strong>¡Versión nueva!</strong> <span>Agarrao {{ c!.ultima }}</span>
        </p>
        <button type="button" class="cinta__actualizar" (click)="actualizar()" [disabled]="actualizando()">
          {{ actualizando() ? 'Un momento…' : 'Actualizar' }}
        </button>
        <button type="button" class="cinta__cerrar" (click)="version.descartar()" aria-label="Ahora no">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
        </button>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
      flex: none;
    }

    .cinta {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: max(6px, env(safe-area-inset-top)) 6px 6px 10px;
      background: var(--rosa-boton);
      color: #fff;
      animation: baja 480ms var(--salida) both;
    }

    .cinta__personaje {
      flex: none;
      width: 36px;
      height: 36px;
      padding: 2px;
      border-radius: 50%;
      background: #fff;
    }

    .cinta__texto {
      flex: 1;
      min-width: 0;
      margin: 0;
      font-size: 0.875rem;
      line-height: 1.2;
    }

    .cinta__texto strong {
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1rem;
    }

    .cinta__texto strong,
    .cinta__texto span { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .cinta__texto span { opacity: 0.92; }

    .cinta__actualizar {
      flex: none;
      min-height: 40px;
      padding: 0 14px;
      border: 0;
      border-radius: 999px;
      background: #fff;
      color: var(--rosa-boton);
      font: inherit;
      font-weight: 800;
      box-shadow: 0 3px 8px -2px rgb(42 47 90 / 0.3);
      cursor: pointer;
    }

    .cinta__actualizar:disabled { opacity: 0.8; cursor: progress; }

    .cinta__cerrar {
      flex: none;
      display: grid;
      place-items: center;
      width: 44px;
      height: 44px;
      border: 0;
      border-radius: 50%;
      background: transparent;
      color: #fff;
      cursor: pointer;
    }

    .cinta__cerrar svg {
      width: 20px;
      height: 20px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.8;
      stroke-linecap: round;
    }

    .cinta :focus-visible { outline-color: #fff; }

    @keyframes baja {
      from { transform: translateY(-100%); }
    }
  `,
})
export class AvisoVersion {
  protected readonly version = inject(VersionService);
  protected readonly actualizando = signal(false);

  protected actualizar(): void {
    this.actualizando.set(true);
    void this.version.actualizar();
  }
}
