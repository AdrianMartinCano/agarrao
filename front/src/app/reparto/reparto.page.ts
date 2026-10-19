import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { comoSeDice, precioPorUnidad, retrato } from '../precios/generico.model';
import { Cadena, NOMBRE_CADENA } from '../tickets/ticket.model';
import { ArticuloRepartido, Reparto, RepartoService } from './reparto.service';

/**
 * ¿Dónde lo compro?: lo que falta de la lista repartido por súper. Cada súper es un
 * resguardo de papel de caja con lo que toca comprar allí y lo que costará; arriba, lo que
 * se ahorra frente a ir a una sola tienda.
 */
@Component({
  selector: 'app-reparto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  templateUrl: './reparto.page.html',
  styleUrl: './reparto.page.scss',
})
export class RepartoPage {
  private readonly hogares = inject(HogaresService);
  private readonly servicio = inject(RepartoService);

  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly retrato = retrato;
  protected readonly decir = comoSeDice;

  protected readonly reparto = signal<Reparto | null>(null);
  protected readonly error = signal<string | null>(null);

  /** Lo que se ahorra repartiendo frente a la mejor tienda sola que lo tiene todo. */
  protected readonly ahorro = computed(() => {
    const r = this.reparto();
    const sola = r?.enUna.find((e) => e.faltan === 0);
    if (!r || !sola || r.grupos.length < 2) return null;
    const dif = sola.total - r.total;
    return dif >= 0.01 ? { cadena: sola.cadena, dif } : null;
  });

  constructor() {
    effect(() => {
      const hogar = this.hogares.activo();
      if (hogar) untracked(() => this.cargar(hogar.id));
    });
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected porUnidad(a: ArticuloRepartido): string {
    return precioPorUnidad(a.precioUnidad, a.unidad);
  }

  protected nombreDe(cadena: Cadena): string {
    return NOMBRE_CADENA[cadena];
  }

  private cargar(hogarId: string): void {
    this.servicio.repartir(hogarId).subscribe({
      next: (r) => this.reparto.set(r),
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha podido repartir la lista.')),
    });
  }
}
