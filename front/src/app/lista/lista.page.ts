import { DOCUMENT } from '@angular/common';
import {
  ApplicationRef,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { IonContent, IonFooter } from '@ionic/angular';
import { filter, fromEvent, interval, merge } from 'rxjs';

import { CINTA_DE_FUERA, cintasPorPersona } from '../hogar/cintas';
import { HogaresService } from '../hogar/hogares.service';
import { Pestanas } from '../pestanas/pestanas';
import { Miembro } from '../hogar/hogar.model';
import { Articulo } from './articulo.model';
import { FilaArticulo } from './fila-articulo';
import { ListaService } from './lista.service';

/** Cada cuánto se mira si los demás han cambiado algo, con la pantalla a la vista. */
const CADA_CUANTO_MS = 10_000;

/**
 * La lista del hogar abierto: lo que falta arriba, lo que ya está en el carro abajo, y el
 * campo de apuntar fijo a mano del pulgar.
 */
@Component({
  selector: 'app-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterLink, FilaArticulo, Pestanas],
  templateUrl: './lista.page.html',
  styleUrl: './lista.page.scss',
})
export class ListaPage {
  protected readonly lista = inject(ListaService);
  private readonly hogares = inject(HogaresService);
  private readonly documento = inject(DOCUMENT);
  private readonly app = inject(ApplicationRef);

  protected readonly hogar = this.hogares.activo;
  private readonly miembros = signal<readonly Miembro[]>([]);
  private readonly cintas = computed(() => cintasPorPersona(this.miembros()));

  protected readonly texto = signal('');
  /** Lo tachado en esta visita: solo esas filas dibujan el trazo (las demás ya lo tienen). */
  protected readonly recien = signal<ReadonlySet<string>>(new Set());

  protected readonly cargando = computed(() => this.lista.articulos() === null && !this.lista.error());
  protected readonly vacia = computed(() => this.lista.articulos()?.length === 0);
  protected readonly todoEnElCarro = computed(
    () => this.lista.pendientes().length === 0 && this.lista.enElCarro().length > 0,
  );

  constructor() {
    // Abrir el hogar activo (y volver a abrir si cambia).
    effect(() => {
      const hogar = this.hogar();
      if (!hogar) return;
      untracked(() => {
        this.lista.abrir(hogar.id);
        this.hogares.detalle(hogar.id).subscribe({
          next: (h) => this.miembros.set(h.miembros),
          error: () => undefined, // Sin miembros, las cintas salen lilas; la lista funciona igual.
        });
      });
    });

    // Lo de los demás: cada 10 s y al volver a la pantalla, solo si se ve.
    const visible = () => this.documento.visibilityState === 'visible';
    merge(interval(CADA_CUANTO_MS), fromEvent(this.documento, 'visibilitychange'))
      .pipe(filter(visible), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.lista.refrescar());
  }

  protected cinta(articulo: Articulo): string {
    return this.cintas().get(articulo.apuntadoPorId) ?? CINTA_DE_FUERA;
  }

  /** Con el campo vacío, el botón no se apaga: lleva al campo (apuntar sin buscarlo). */
  protected apuntar(evento: Event, campo: HTMLInputElement): void {
    evento.preventDefault();
    const texto = this.texto().trim();
    if (!texto) {
      campo.focus();
      return;
    }
    this.lista.apuntar(texto);
    this.texto.set('');
  }

  /**
   * La firma de la pantalla: con View Transitions la fila viaja de su sitio al carro (o
   * vuelve) y las de alrededor cierran el hueco. Sin soporte, o con «reducir movimiento»,
   * el cambio es directo y la fila solo hace una caída corta.
   */
  protected marcar(articulo: Articulo, comprado: boolean): void {
    const cambiar = () => {
      if (comprado) this.recien.update((s) => new Set(s).add(articulo.id));
      this.lista.marcar(articulo, comprado);
      // Zoneless: pintar ya, dentro de la transición, y no en el siguiente ciclo.
      this.app.tick();
    };
    const sinMovimiento = matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (sinMovimiento || !this.documento.startViewTransition) {
      cambiar();
      return;
    }
    this.documento.startViewTransition(cambiar);
  }

  protected valor(evento: Event): string {
    return evento.target instanceof HTMLInputElement ? evento.target.value : '';
  }
}
