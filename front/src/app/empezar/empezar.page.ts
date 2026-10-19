import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import { mensajeDeError } from '../core/api';
import { Invitacion } from '../hogar/hogar.model';
import { HogaresService } from '../hogar/hogares.service';

type Camino = 'crear' | 'unirse';

interface Busqueda {
  hogar?: Invitacion;
  buscando?: boolean;
  error?: string;
}

/**
 * Primera vez, o un hogar más: crear el mío o entrar en uno con código. Dos fichas del
 * catálogo, una por camino; la elegida se abre con su campo.
 *
 * Con `?codigo=` (llegó por un enlace de invitación) se abre directamente «Tengo un
 * código» con él escrito: la persona solo confirma.
 */
@Component({
  selector: 'app-empezar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent],
  templateUrl: './empezar.page.html',
  styleUrl: './empezar.page.scss',
})
export class EmpezarPage {
  private readonly hogares = inject(HogaresService);
  private readonly router = inject(Router);

  /** Del enlace de invitación (query param). */
  readonly codigo = input<string>();

  private readonly elegido = signal<Camino | null>(null);
  /** Con código en el enlace, se empieza ya en «unirse». */
  protected readonly camino = computed<Camino | null>(() => this.elegido() ?? (this.codigo() ? 'unirse' : null));

  protected readonly nombre = signal('Mi casa');
  private readonly codigoEscrito = signal<string | null>(null);
  /** Agrupado «ABCD 2345» mientras se escribe, igual que en el cartel del hogar. */
  protected readonly codigoCampo = computed(() => agrupar(this.codigoEscrito() ?? this.codigo() ?? ''));

  private readonly codigoLimpio = computed(() => this.codigoCampo().replace(/\s/g, ''));
  /**
   * Con el código completo, a qué hogar lleva: se ve el nombre antes de entrar. Si el
   * código no existe, el error sale aquí mismo y no al pulsar.
   */
  // Con RxJS y no con httpResource: este suma 10 kB al paquete inicial.
  private readonly busqueda = toSignal(
    toObservable(
      computed(() => (this.camino() === 'unirse' && this.codigoLimpio().length === 8 ? this.codigoLimpio() : null)),
    ).pipe(
      switchMap((codigo) =>
        codigo
          ? this.hogares.invitacion(codigo).pipe(
              map((hogar): Busqueda => ({ hogar })),
              catchError((e: unknown) => of<Busqueda>({ error: mensajeDeError(e, 'No se ha podido comprobar el código.') })),
              startWith<Busqueda>({ buscando: true }),
            )
          : of<Busqueda>({}),
      ),
    ),
    { initialValue: {} as Busqueda },
  );
  protected readonly hogarInvitado = computed(() => this.busqueda().hogar ?? null);
  protected readonly buscandoInvitacion = computed(() => this.busqueda().buscando === true);
  protected readonly errorInvitacion = computed(() => this.busqueda().error ?? null);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Ya tiene algún hogar: esta pantalla es «otro más» y se puede volver. */
  protected readonly tieneHogar = computed(() => (this.hogares.hogares()?.length ?? 0) > 0);

  protected elegir(camino: Camino): void {
    this.elegido.set(camino);
    this.error.set(null);
  }

  protected escribirCodigo(valor: string): void {
    this.codigoEscrito.set(valor);
  }

  protected valor(evento: Event): string {
    return evento.target instanceof HTMLInputElement ? evento.target.value : '';
  }

  protected crear(evento: Event): void {
    evento.preventDefault();
    const nombre = this.nombre().trim();
    if (!nombre) {
      this.error.set('Ponle un nombre a tu hogar.');
      return;
    }
    this.lanzar(() => this.hogares.crear(nombre));
  }

  protected unirse(evento: Event): void {
    evento.preventDefault();
    const codigo = this.codigoCampo().trim();
    if (!codigo) {
      this.error.set('Escribe el código que te han pasado.');
      return;
    }
    this.lanzar(() => this.hogares.unirse(codigo));
  }

  protected volver(): void {
    void this.router.navigateByUrl('/lista');
  }

  private lanzar(accion: () => ReturnType<HogaresService['crear']>): void {
    this.enviando.set(true);
    this.error.set(null);
    accion().subscribe({
      next: () => void this.router.navigateByUrl('/lista', { replaceUrl: true }),
      error: (e: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }
}

function agrupar(codigo: string): string {
  const limpio = codigo.replace(/[\s-]/g, '').toUpperCase().slice(0, 8);
  return limpio.length > 4 ? `${limpio.slice(0, 4)} ${limpio.slice(4)}` : limpio;
}
