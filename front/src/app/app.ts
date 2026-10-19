import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { IonApp, IonRouterOutlet } from '@ionic/angular';

import { PerfilService } from './core/perfil';
import { VersionService } from './core/version.service';
import { AvisoVersion } from './version/aviso-version';
import { VersionObligatoria } from './version/version-obligatoria';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonApp, IonRouterOutlet, AvisoVersion, VersionObligatoria],
  template: `
    <ion-app [class.con-aviso]="conAviso()">
      @if (bloqueada()) {
        <app-version-obligatoria />
      } @else {
        <app-aviso-version />
        <!-- El outlet ocupa lo que deje la cinta: así la cinta empuja la página, no la tapa -->
        <ion-router-outlet class="salida" />
      }
    </ion-app>
  `,
  styles: `
    .salida {
      position: relative;
      flex: 1 1 auto;
    }
  `,
})
export class App {
  private readonly version = inject(VersionService);

  protected readonly bloqueada = computed(() => this.version.comprobacion()?.estado === 'OBLIGATORIA');
  /** Con la cinta puesta las pantallas tienen menos alto y algunas se compactan. */
  protected readonly conAviso = computed(() => this.version.comprobacion()?.estado === 'OPCIONAL');

  constructor() {
    // Si falla (sin red, back caído) no se bloquea nada: la app tiene que abrir en el súper
    // sin cobertura.
    this.version.comprobar().subscribe({ error: () => undefined });

    // Al entrar, ng-auth pone el email como nombre (el login no lo trae): se corrige solo.
    const perfil = inject(PerfilService);
    effect(() => perfil.recuperarNombre());
  }
}
