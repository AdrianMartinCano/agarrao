import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IonContent, IonFooter } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { ListaService } from '../lista/lista.service';
import { Pestanas } from '../pestanas/pestanas';
import { ClasificarLinea, Eleccion } from '../tickets/clasificar-linea';
import { Cadena, NOMBRE_CADENA, Unidad } from '../tickets/ticket.model';
import { CaraACara } from './cara-a-cara';
import {
  Busqueda,
  CATEGORIAS,
  Categoria,
  GenericoDetalle,
  GenericoResumen,
  NOMBRE_CATEGORIA,
  ProductoDeGenerico,
  ProductoBuscado,
  ProductoWeb,
  EnCadaSuper,
  comoSeDice,
  precioPorUnidad,
  retrato,
  diaCorto,
} from './generico.model';
import { GenericosService } from './genericos.service';

interface Pasillo {
  categoria: Categoria;
  genericos: GenericoResumen[];
}

/**
 * Precios: el catálogo de genéricos por pasillo, con su mejor €/unidad sacado de los tickets
 * del hogar. Al abrir uno, sus productos de cada cadena se enfrentan «cara a cara».
 */
@Component({
  selector: 'app-precios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonContent,
    IonFooter,
    NgTemplateOutlet,
    RouterLink,
    Pestanas,
    CaraACara,
    ClasificarLinea,
  ],
  templateUrl: './precios.page.html',
  styleUrl: './precios.page.scss',
})
export class PreciosPage {
  private readonly hogares = inject(HogaresService);
  private readonly genericos = inject(GenericosService);
  private readonly lista = inject(ListaService);

  protected readonly nombreCategoria = NOMBRE_CATEGORIA;
  protected readonly categorias = CATEGORIAS;
  protected readonly retrato = retrato;
  protected readonly decir = comoSeDice;

  protected readonly catalogo = signal<GenericoResumen[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly busqueda = signal('');
  protected readonly pasillo = signal<Categoria | null>(null);
  protected readonly verSinPrecio = signal(false);

  /** El genérico abierto y su comparación (null mientras llega). */
  protected readonly abierto = signal<string | null>(null);
  protected readonly detalle = signal<GenericoDetalle | null>(null);
  protected readonly errorDetalle = signal<string | null>(null);
  /** El producto con el panel de corregir abierto (cadena + texto). */
  protected readonly corrigiendo = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorCorregir = signal<string | null>(null);
  protected readonly confirmandoBorrar = signal(false);
  /** Los productos de la web se enseñan de ocho en ocho (son decenas). */
  protected readonly verTodaLaWeb = signal(false);

  // Crear un genérico propio.
  protected readonly creando = signal(false);
  protected readonly nuevoNombre = signal('');
  protected readonly nuevaCategoria = signal<Categoria>('OTROS');
  protected readonly nuevaUnidad = signal<Unidad>('UD');
  protected readonly errorCrear = signal<string | null>(null);

  private readonly hogarId = computed(() => this.hogares.activo()?.id ?? null);

  /** Lo buscado en las webs de los súper (desde 3 letras, al dejar de escribir). Null: nada buscado. */
  protected readonly enLasTiendas = signal<Busqueda | null>(null);
  protected readonly buscandoEnTiendas = signal(false);
  /** La cadena con la lista abierta (los cinco más baratos). */
  protected readonly tiendaAbierta = signal<string | null>(null);
  /** El producto del buscador que se ha tocado, y ese producto en cada súper. */
  protected readonly elegido = signal<{ cadena: Cadena; producto: ProductoBuscado } | null>(null);
  protected readonly enCadaSuper = signal<EnCadaSuper | null>(null);
  protected readonly cargandoCada = signal(false);
  protected readonly errorCada = signal<string | null>(null);
  /** Lo apuntado desde aquí en esta visita, para que el botón diga «En la lista». */
  protected readonly apuntados = signal<ReadonlySet<string>>(new Set());
  protected readonly errorLista = this.lista.error;

  protected readonly conPrecio = computed(() =>
    (this.catalogo() ?? []).filter((g) => g.mejorPrecio !== null),
  );
  protected readonly sinPrecios = computed(
    () => this.catalogo() !== null && this.conPrecio().length === 0,
  );

  /** Lo que se ve según el buscador y el pasillo elegido. */
  private readonly filtrados = computed(() => {
    const q = normalizar(this.busqueda());
    const pasillo = this.pasillo();
    return (this.catalogo() ?? []).filter(
      (g) => (!pasillo || g.categoria === pasillo) && (!q || normalizar(g.nombre).includes(q)),
    );
  });

  /** Filtrando o buscando, todo junto: lo que se busca puede no tener precio todavía. */
  protected readonly filtrando = computed(
    () => !!this.busqueda().trim() || this.pasillo() !== null,
  );

  /** Lo que compra el hogar (sale en sus tickets), primero. */
  protected readonly pasillosComprados = computed(() =>
    porPasillo(this.filtrados().filter((g) => g.productos > 0)),
  );
  /** El resto con precio (de las webs de los súper). */
  protected readonly pasillosConPrecio = computed(() =>
    porPasillo(this.filtrados().filter((g) => g.productos === 0 && g.mejorPrecio !== null)),
  );
  protected readonly pasillosSinPrecio = computed(() =>
    porPasillo(this.filtrados().filter((g) => g.mejorPrecio === null)),
  );
  protected readonly cuantosSinPrecio = computed(
    () => this.filtrados().filter((g) => g.mejorPrecio === null).length,
  );
  protected readonly noHayNada = computed(
    () =>
      this.pasillosComprados().length +
        this.pasillosConPrecio().length +
        this.pasillosSinPrecio().length ===
      0,
  );

  constructor() {
    effect(() => {
      const id = this.hogarId();
      if (id) untracked(() => this.cargar(id));
    });

    toObservable(this.busqueda)
      .pipe(
        map((q) => q.trim()),
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((q) => {
          const hogar = this.hogarId();
          this.tiendaAbierta.set(null);
          this.cerrarEnCadaSuper();
          if (!hogar || normalizar(q).replace(/\s/g, '').length < 3) {
            this.buscandoEnTiendas.set(false);
            return of(null);
          }
          this.buscandoEnTiendas.set(true);
          return this.genericos.buscar(hogar, q).pipe(catchError(() => of(null)));
        }),
      )
      .subscribe((r) => {
        this.buscandoEnTiendas.set(false);
        this.enLasTiendas.set(r);
      });
  }

  protected verEnCadaSuper(cadena: Cadena, producto: ProductoBuscado): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    this.elegido.set({ cadena, producto });
    this.enCadaSuper.set(null);
    this.errorCada.set(null);
    this.cargandoCada.set(true);
    this.genericos.enCadaSuper(hogar, cadena, producto.idExterno).subscribe({
      next: (r) => {
        this.cargandoCada.set(false);
        this.enCadaSuper.set(r);
      },
      error: (e: unknown) => {
        this.cargandoCada.set(false);
        this.errorCada.set(mensajeDeError(e));
      },
    });
  }

  /** Apunta en la lista del hogar el genérico («Aguacate») o el producto concreto del buscador. */
  protected apuntar(nombre: string): void {
    const hogar = this.hogarId();
    const texto = nombre.trim().slice(0, 80).trim();
    if (!hogar || !texto) return;
    this.lista.abrir(hogar);
    this.lista.apuntar(texto);
    this.apuntados.update((ya) => new Set(ya).add(texto));
  }

  protected yaApuntado(nombre: string): boolean {
    return this.apuntados().has(nombre.trim().slice(0, 80).trim());
  }

  protected cerrarEnCadaSuper(): void {
    this.elegido.set(null);
    this.enCadaSuper.set(null);
    this.errorCada.set(null);
  }

  protected abrirTienda(cadena: string): void {
    this.tiendaAbierta.update((c) => (c === cadena ? null : cadena));
  }

  protected precio(g: { mejorPrecio: number | null; unidad: Unidad }): string | null {
    return g.mejorPrecio === null ? null : precioPorUnidad(g.mejorPrecio, g.unidad);
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected dia(fecha: string): string {
    return diaCorto(fecha);
  }

  protected nombreDe(cadena: Cadena): string {
    return NOMBRE_CADENA[cadena];
  }

  protected elegirPasillo(c: Categoria | null): void {
    this.pasillo.update((actual) => (actual === c ? null : c));
  }

  protected abrir(g: GenericoResumen): void {
    this.corrigiendo.set(null);
    this.verTodaLaWeb.set(false);
    this.confirmandoBorrar.set(false);
    if (this.abierto() === g.id) {
      this.abierto.set(null);
      return;
    }
    this.abierto.set(g.id);
    this.cargarDetalle(g.id);
  }

  protected claveProducto(p: ProductoDeGenerico): string {
    return `${p.cadena}·${p.texto}`;
  }

  protected corregir(p: ProductoDeGenerico): void {
    this.errorCorregir.set(null);
    this.corrigiendo.update((c) => (c === this.claveProducto(p) ? null : this.claveProducto(p)));
  }

  /** Un producto de la web que no es este genérico: fuera de la comparación. */
  protected noEsEste(d: GenericoDetalle, p: ProductoWeb): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    this.genericos
      .clasificarWeb(hogar, p.cadena, p.idExterno, null, d.propio ? d.id : null)
      .subscribe({
        next: () => {
          this.cargar(hogar);
          this.cargarDetalle(d.id);
        },
        error: (e: unknown) => this.errorDetalle.set(mensajeDeError(e)),
      });
  }

  protected guardarCorreccion(p: ProductoDeGenerico, eleccion: Eleccion): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    this.guardando.set(true);
    this.genericos.clasificar(hogar, { cadena: p.cadena, texto: p.texto, ...eleccion }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.corrigiendo.set(null);
        this.cargar(hogar);
        const abierto = this.abierto();
        if (abierto) this.cargarDetalle(abierto);
      },
      error: (e: unknown) => {
        this.guardando.set(false);
        this.errorCorregir.set(mensajeDeError(e));
      },
    });
  }

  protected borrar(g: GenericoDetalle): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    if (!this.confirmandoBorrar()) {
      this.confirmandoBorrar.set(true);
      return;
    }
    this.genericos.borrar(hogar, g.id).subscribe({
      next: () => {
        this.abierto.set(null);
        this.confirmandoBorrar.set(false);
        this.cargar(hogar);
      },
      error: (e: unknown) => this.errorDetalle.set(mensajeDeError(e)),
    });
  }

  protected crear(evento: Event): void {
    evento.preventDefault();
    const hogar = this.hogarId();
    const nombre = this.nuevoNombre().trim();
    if (!hogar) return;
    if (!nombre) {
      this.errorCrear.set('Ponle un nombre: «Turrón», «Comida del gato»…');
      return;
    }
    this.genericos.crear(hogar, nombre, this.nuevaCategoria(), this.nuevaUnidad()).subscribe({
      next: (g) => {
        this.creando.set(false);
        this.nuevoNombre.set('');
        this.errorCrear.set(null);
        this.busqueda.set(g.nombre);
        this.pasillo.set(null);
        this.cargar(hogar);
      },
      error: (e: unknown) => this.errorCrear.set(mensajeDeError(e)),
    });
  }

  protected valor(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLSelectElement).value;
  }

  private cargar(hogarId: string): void {
    this.genericos.catalogo(hogarId).subscribe({
      next: (c) => {
        this.catalogo.set(c);
        this.error.set(null);
      },
      error: (e: unknown) =>
        this.error.set(mensajeDeError(e, 'No se han podido cargar los precios.')),
    });
  }

  private cargarDetalle(id: string): void {
    const hogar = this.hogarId();
    if (!hogar) return;
    this.detalle.set(null);
    this.errorDetalle.set(null);
    this.genericos.detalle(hogar, id).subscribe({
      next: (d) => {
        if (this.abierto() === d.id) this.detalle.set(d);
      },
      error: (e: unknown) => this.errorDetalle.set(mensajeDeError(e)),
    });
  }
}

function porPasillo(genericos: GenericoResumen[]): Pasillo[] {
  return CATEGORIAS.map((categoria) => ({
    categoria,
    genericos: genericos.filter((g) => g.categoria === categoria),
  })).filter((p) => p.genericos.length > 0);
}

function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}
