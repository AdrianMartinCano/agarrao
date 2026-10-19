import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AuthService, GoogleAuthService, GoogleButtonComponent } from '@adrianmartincano/ng-auth';
import { IonContent } from '@ionic/angular';
import { map } from 'rxjs';

import { ACCESO_PRUEBA } from '../core/acceso-prueba';
import { VersionService } from '../core/version.service';
import { tomarInvitacion } from '../hogar/invitacion';
import { FichaPersonaje, Personaje } from '../personajes/ficha-personaje';

/**
 * El elenco de la portada. PRECIOS DE EJEMPLO, y así lo dice la pantalla: enseñan cómo
 * compara la app (precio por unidad, marca blanca contra marca blanca), no precios reales.
 * Por eso las tiendas son «Súper A» y «Súper B» y no cadenas con nombre.
 */
const ELENCO: readonly Personaje[] = [
  {
    puesto: 1,
    nombre: 'Listillo',
    precio: null,
    etiqueta: 'Compara por ti',
    nota: 'Apunta cada ticket. Y no se le olvida.',
    imagen: 'listillo.svg',
    tono: 'lila',
    cinta: 'listillo',
    placa: 'rosa',
  },
  {
    puesto: 2,
    nombre: 'Leche semi',
    precio: null,
    comparativa: [
      { donde: 'Súper A', precio: '0,85 €/L', mejor: true },
      { donde: 'Súper B', precio: '0,92 €/L' },
    ],
    nota: 'Blanca contra blanca.',
    imagen: 'leche.svg',
    tono: 'cielo',
    cinta: 'leche',
    placa: 'lila',
  },
  {
    puesto: 3,
    nombre: 'Huevos L',
    precio: '0,21 €/ud',
    nota: 'Cuenta por huevo, no por caja.',
    imagen: 'huevo.svg',
    tono: 'mantequilla',
    cinta: 'huevo',
    placa: 'rosa',
  },
  {
    puesto: 4,
    nombre: 'Tomate frito',
    precio: '2,35 €/kg',
    nota: 'Por kilo, que los botes engañan.',
    imagen: 'tomate-frito.svg',
    tono: 'rubor',
    cinta: 'tomate',
    placa: 'lila',
  },
];

@Component({
  selector: 'app-bienvenida',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, GoogleButtonComponent, FichaPersonaje, RouterLink],
  templateUrl: './bienvenida.page.html',
  styleUrl: './bienvenida.page.scss',
})
export class BienvenidaPage {
  private readonly router = inject(Router);
  private readonly google = inject(GoogleAuthService);
  private readonly auth = inject(AuthService);
  protected readonly version = inject(VersionService).actual;

  protected readonly elenco = ELENCO;

  /** El personaje elegido lleva su cinta de color. Empieza Listillo, el número 1. */
  protected readonly elegido = signal(1);

  /** Si el back tiene Google apagado, el botón no aparece y hay que decir por qué. */
  protected readonly hayGoogle = toSignal(this.google.config().pipe(map((c) => c.enabled)), {
    initialValue: true,
  });

  /** Solo en `ng serve` (ver core/acceso-prueba.ts): en producción es null y no se pinta. */
  protected readonly accesoPrueba = ACCESO_PRUEBA;
  protected readonly entrandoDePrueba = signal(false);
  protected readonly errorPrueba = signal<string | null>(null);

  /** Con una invitación pendiente (llegó por enlace sin sesión), a confirmarla. */
  protected entrar(): void {
    const codigo = tomarInvitacion();
    void this.router.navigate(codigo ? ['/empezar'] : ['/lista'], {
      queryParams: codigo ? { codigo } : {},
      replaceUrl: true,
    });
  }

  protected entrarDePrueba(): void {
    if (!this.accesoPrueba) return;
    this.entrandoDePrueba.set(true);
    this.errorPrueba.set(null);
    this.auth.login(this.accesoPrueba).subscribe({
      next: () => this.entrar(),
      error: () => {
        this.entrandoDePrueba.set(false);
        this.errorPrueba.set('No entra: ¿está arrancado el back con ListilloLocal?');
      },
    });
  }
}
