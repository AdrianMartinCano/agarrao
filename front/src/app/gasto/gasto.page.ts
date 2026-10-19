import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent, IonFooter } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { Pestanas } from '../pestanas/pestanas';
import { Categoria, NOMBRE_CATEGORIA, retrato } from '../precios/generico.model';
import { NOMBRE_CADENA } from '../tickets/ticket.model';
import { COLOR_TIPO, Gasto, GastoAnotado, GastoFijo, GastoService, NOMBRE_TIPO } from './gasto.service';
import { Capa, Hucha } from './hucha';

/**
 * El color de cada pasillo en el tarro: un tono distinto por pasillo, sin dos de la misma
 * familia y ninguno parecido al cristal vacío (#f4f0ff), para que se lea de un vistazo.
 */
const COLOR_CATEGORIA: Record<Categoria | 'SIN' | 'OTRAS', string> = {
  FRUTA: '#ff8fab',
  VERDURA: '#7fd6a0',
  CARNE: '#ff9f6e',
  PESCADO: '#6fb8f0',
  LACTEOS: '#fff1c1',
  PANADERIA: '#d9a36a',
  DESPENSA: '#f4c84f',
  DESAYUNO: '#b98ce6',
  BEBIDAS: '#5fd3d0',
  CONGELADOS: '#c4ecf8',
  LIMPIEZA: '#9da5f2',
  HIGIENE: '#cfd86e',
  HOGAR: '#8fb3a8',
  OTROS: '#c9c2b8',
  OTRAS: '#c9c2b8',
  SIN: '#a9a5bd',
};

/** Por debajo de esto un pasillo no se lee en el tarro: se junta en «Otras cosas». */
const CAPA_MINIMA = 0.04;

/**
 * Gasto: el mes como un tarro de hucha que se llena por capas. Si en el mes solo hay súper,
 * una capa por pasillo; si hay más cosas (restaurantes, gasolina, suscripciones…), una por
 * tipo, y los pasillos pasan a su propio bloque. Debajo, los seis últimos meses como
 * tarritos (se tocan para elegir), lo anotado, lo que se repite cada mes, las tiendas y lo
 * que más cuesta.
 */
@Component({
  selector: 'app-gasto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterLink, Pestanas, Hucha],
  templateUrl: './gasto.page.html',
  styleUrl: './gasto.page.scss',
})
export class GastoPage {
  private readonly hogares = inject(HogaresService);
  private readonly gastos = inject(GastoService);

  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly nombreTipo = NOMBRE_TIPO;
  protected readonly nombreCategoria = NOMBRE_CATEGORIA;
  protected readonly colorCategoria = COLOR_CATEGORIA;
  protected readonly colorTipo = COLOR_TIPO;
  protected readonly retrato = retrato;

  /** De la ruta (?mes=2026-09): al volver de anotar un gasto, su mes. */
  readonly mes = input<string>();

  protected readonly gasto = signal<Gasto | null>(null);
  protected readonly anotados = signal<GastoAnotado[]>([]);
  protected readonly fijos = signal<GastoFijo[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly quitandoFijo = signal<string | null>(null);
  /** Null: el mes actual. */
  private readonly mesElegido = signal<string | null>(null);
  private mesDeLaRutaLeido = false;

  /** Hay algo más que súper este mes: la hucha va por tipo y los pasillos aparte. */
  protected readonly porTipo = computed(() => {
    const tipos = this.gasto()?.porTipo ?? [];
    return tipos.length > 1 || (tipos.length === 1 && tipos[0].tipo !== 'SUPER');
  });

  private readonly hogarId = computed(() => this.hogares.activo()?.id ?? null);

  /** El tarro lleno: el mes más caro de los últimos doce (fijo, mires el mes que mires). */
  private readonly maximo = computed(() => this.gasto()?.referencia ?? 0);

  /** Tipos (o pasillos, si solo hay súper) de mayor a menor; los de menos del 4 % se juntan al final. */
  protected readonly capas = computed((): Capa[] => {
    const g = this.gasto();
    if (!g) return [];
    if (this.porTipo()) {
      return g.porTipo.map((t) => ({
        clave: t.tipo,
        nombre: NOMBRE_TIPO[t.tipo],
        total: t.total,
        color: COLOR_TIPO[t.tipo],
      }));
    }
    const suma = g.porCategoria.reduce((s, c) => s + c.total, 0);
    const capas: Capa[] = [];
    let otras = 0;
    for (const c of g.porCategoria) {
      if (suma > 0 && c.total / suma < CAPA_MINIMA) {
        otras += c.total;
      } else {
        capas.push({
          clave: c.categoria ?? 'SIN',
          nombre: c.categoria ? NOMBRE_CATEGORIA[c.categoria] : 'Sin clasificar',
          total: c.total,
          color: COLOR_CATEGORIA[c.categoria ?? 'SIN'],
        });
      }
    }
    if (otras > 0) capas.push({ clave: 'OTRAS', nombre: 'Otras cosas', total: otras, color: COLOR_CATEGORIA.OTRAS });
    return capas;
  });

  private readonly sumaCapas = computed(() => this.capas().reduce((s, c) => s + c.total, 0));

  protected readonly nivel = computed(() => {
    const g = this.gasto();
    const max = this.maximo();
    // Un mínimo para que un mes flojo no parezca vacío.
    return g && max > 0 && g.total > 0 ? Math.min(1, Math.max(0.12, g.total / max)) : 0;
  });

  protected readonly etiquetaHucha = computed(() => {
    const g = this.gasto();
    if (!g) return '';
    const capas = this.capas().map((c) => `${c.nombre} ${this.euros(c.total)} €`).join(', ');
    return `${this.nombreMes(g.mes, true)}: ${this.euros(g.total)} €${capas ? '. ' + capas : ''}`;
  });

  /** La nota de Listillo: qué tipo o pasillo se come la hucha, si alguno destaca. */
  protected readonly goloso = computed(() => {
    const capas = this.capas();
    const [primera] = capas;
    const suma = this.sumaCapas();
    // Con una sola capa no hay nada que destacar: es todo.
    if (capas.length < 2 || !primera || suma <= 0 || primera.clave === 'SIN' || primera.clave === 'OTRAS') return null;
    const parte = primera.total / suma;
    if (parte < 0.3) return null;
    const cuanto = parte >= 0.5 ? 'más de la mitad' : parte >= 0.4 ? 'casi la mitad' : 'un tercio';
    return `${primera.nombre} se come ${cuanto} de la hucha.`;
  });

  protected readonly esteMes = computed(() => this.gasto()?.mes === mesDeHoy());
  protected readonly maxTienda = computed(() => Math.max(0, ...(this.gasto()?.porTienda.map((t) => t.total) ?? [])));

  constructor() {
    effect(() => {
      const hogar = this.hogarId();
      if (!this.mesDeLaRutaLeido) {
        this.mesDeLaRutaLeido = true;
        const deLaRuta = untracked(() => this.mes());
        if (deLaRuta && /^\d{4}-\d{2}$/.test(deLaRuta) && deLaRuta < mesDeHoy()) {
          untracked(() => this.mesElegido.set(deLaRuta));
        }
      }
      const mes = this.mesElegido();
      if (hogar) untracked(() => this.cargar(hogar, mes));
    });
  }

  protected elegirMes(mes: string): void {
    this.mesElegido.set(mes === mesDeHoy() ? null : mes);
  }

  protected moverMes(delta: number): void {
    const g = this.gasto();
    if (!g) return;
    const [a, m] = g.mes.split('-').map(Number);
    const fecha = new Date(a, m - 1 + delta, 1);
    const nuevo = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
    if (nuevo > mesDeHoy()) return;
    this.elegirMes(nuevo);
  }

  protected porcentaje(total: number): string {
    const suma = this.sumaCapas();
    return suma > 0 ? `${Math.round((total * 100) / suma)} %` : '';
  }

  /** Lo lleno de cada tarrito de mes, de 0 a 1. */
  protected nivelMes(total: number): number {
    const max = this.maximo();
    return max > 0 ? Math.min(1, total / max) : 0;
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected redondo(valor: number): string {
    return Math.round(valor).toLocaleString('es-ES');
  }

  /** «octubre», «oct» o «octubre de 2026». */
  protected nombreMes(mes: string, largo = false, conAnio = false): string {
    const [a, m] = mes.split('-').map(Number);
    const fecha = new Date(a, m - 1, 15);
    return fecha.toLocaleDateString('es-ES', conAnio ? { month: 'long', year: 'numeric' } : { month: largo ? 'long' : 'short' }).replace('.', '');
  }

  /** Que deje de apuntarse cada mes (dos toques). Lo ya apuntado se queda. */
  protected quitarFijo(fijo: GastoFijo): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    if (this.quitandoFijo() !== fijo.id) {
      this.quitandoFijo.set(fijo.id);
      return;
    }
    this.gastos.quitarFijo(hogar, fijo.id).subscribe({
      next: () => {
        this.quitandoFijo.set(null);
        this.fijos.update((fs) => fs.filter((f) => f.id !== fijo.id));
      },
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha podido quitar.')),
    });
  }

  /** «2 tickets y 3 gastos anotados». */
  protected cuantos(g: Gasto): string {
    const partes: string[] = [];
    if (g.tickets > 0) partes.push(g.tickets === 1 ? 'un ticket' : `${g.tickets} tickets`);
    if (g.anotados > 0) partes.push(g.anotados === 1 ? 'un gasto anotado' : `${g.anotados} gastos anotados`);
    return partes.length === 0 ? 'nada todavía' : 'en ' + partes.join(' y ');
  }

  /** «8 oct». */
  protected dia(fecha: string): string {
    return new Date(`${fecha}T12:00:00`).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' }).replace('.', '');
  }

  private cargar(hogarId: string, mes: string | null): void {
    this.error.set(null);
    this.gastos.resumen(hogarId, mes ?? undefined).subscribe({
      next: (g) => this.gasto.set(g),
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha podido cargar el gasto.')),
    });
    this.gastos.anotados(hogarId, mes ?? undefined).subscribe({
      next: (a) => this.anotados.set(a),
      error: () => this.anotados.set([]),
    });
    this.gastos.fijos(hogarId).subscribe({
      next: (f) => this.fijos.set(f),
      error: () => this.fijos.set([]),
    });
  }
}

function mesDeHoy(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`;
}
