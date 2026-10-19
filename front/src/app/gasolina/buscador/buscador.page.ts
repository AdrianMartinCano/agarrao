import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  Coordenadas,
  EstacionCercana,
  FiltrosBusqueda,
  Lugar,
  OrdenBusqueda,
} from '../core/api.model';
import { AuthService } from '@adrianmartincano/ng-auth';
import { EstacionesService } from '../core/estaciones.service';
import { FavoritosService } from '../core/favoritos.service';
import { FiltrosStore, RADIOS } from '../core/filtros.store';
import { GeolocalizacionService, POSICION_POR_DEFECTO } from '../core/geolocalizacion.service';
import { BuscadorLugar } from './buscador-lugar';
import { ListaEstaciones } from './lista-estaciones';
import { MapaEstaciones } from './mapa-estaciones';

/** Resultados por página. Suficientes para comparar sin marear con una lista infinita. */
const TAMANO_PAGINA = 20;

/**
 * Página principal: buscar gasolineras alrededor de un punto.
 *
 * Es el componente contenedor. Tiene el estado (dónde estamos, qué filtros hay puestos)
 * y habla con los servicios; la lista y el mapa solo reciben datos y avisan de clics.
 */
@Component({
  selector: 'app-buscador',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BuscadorLugar, ListaEstaciones, MapaEstaciones],
  templateUrl: './buscador.page.html',
  styleUrl: './buscador.page.scss',
})
export class BuscadorPage {
  private readonly estacionesService = inject(EstacionesService);
  private readonly geolocalizacion = inject(GeolocalizacionService);
  private readonly favoritosService = inject(FavoritosService);
  private readonly filtrosStore = inject(FiltrosStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  // takeUntilDestroyed() sin argumento solo vale dentro del constructor. Como se
  // suscribe también desde métodos que dispara el usuario (localizar, alternarFavorito),
  // hace falta la referencia explícita.
  private readonly destroyRef = inject(DestroyRef);

  // Opcional porque el contenedor solo existe cuando hay resultados pintados: mientras
  // se busca o si la búsqueda falla, ese @if no ha creado nada.
  private readonly listaScroll = viewChild<ElementRef<HTMLElement>>('listaScroll');

  protected readonly radios = RADIOS;

  protected readonly posicion = signal<Coordenadas>(POSICION_POR_DEFECTO);
  protected readonly ubicacionReal = signal(false);
  protected readonly aviso = signal<string | null>(null);
  protected readonly seleccionada = signal<string | null>(null);
  protected readonly pagina = signal(0);

  /** Municipio o CP elegido a mano, para poder decir desde dónde se está buscando. */
  protected readonly lugarElegido = signal<string | null>(null);

  // Los filtros viven en un servicio, no aquí: este componente se destruye al ir a
  // favoritas, y con el estado dentro se perdían al volver.
  protected readonly filtros = this.filtrosStore.filtros;

  /** Con sesión de Agarrao: favoritas primero y estrella para guardarlas. */
  protected readonly autenticado = computed(() => !!this.auth.currentUser());
  protected readonly favoritos = this.favoritosService.idsFavoritos;

  /**
   * Lista de carburantes para el desplegable.
   *
   * Si la petición falla se queda vacía en vez de tumbar la página: se podrá buscar
   * igual, solo que sin poder cambiar de carburante.
   */
  protected readonly carburantes = toSignal(
    this.estacionesService.carburantes().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  /**
   * La búsqueda en sí.
   *
   * `rxResource` vuelve a lanzarla sola cada vez que cambian la posición o los filtros,
   * y cancela la anterior si aún estaba en vuelo. Es lo que evita el clásico problema de
   * que una respuesta lenta llegue después de una rápida y pise los resultados buenos
   * con los viejos.
   */
  protected readonly busqueda = rxResource({
    params: () => ({
      posicion: this.posicion(),
      filtros: this.filtros(),
      pagina: this.pagina(),
    }),
    stream: ({ params }) =>
      this.estacionesService.cercanas(
        params.posicion,
        params.filtros,
        params.pagina,
        TAMANO_PAGINA,
      ),
  });

  constructor() {
    this.localizar();

    // Los favoritos se cargan solo si hay sesión: para el resto, la estrella ni se pinta.
    if (this.autenticado()) {
      this.favoritosService
        .listar(null, null)
        .pipe(
          catchError(() => of([])),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    }
  }

  /** Pide la ubicación al navegador. Si no la da, se sigue con el centro de Madrid. */
  protected localizar(): void {
    this.aviso.set(null);

    this.geolocalizacion
      .posicionActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (posicion) => {
          this.posicion.set(posicion);
          this.ubicacionReal.set(true);
          this.lugarElegido.set(null);
          // Cambiar de sitio son resultados nuevos: la página en la que estabas no
          // significa nada respecto a ellos.
          this.irAPagina(0);
        },
        error: (error: Error) => {
          // No es un error de la aplicación: se avisa y se busca en el sitio por
          // defecto, que es mucho mejor que dejar la pantalla en blanco.
          this.aviso.set(error.message);
          this.ubicacionReal.set(false);
        },
      });
  }

  protected cambiarCarburante(clave: string): void {
    this.cambiarFiltros((actuales) => ({
      ...actuales,
      carburante: clave || null,
      // Ordenar por precio exige un carburante: si lo quitan, el backend respondería un
      // 400. Se vuelve a distancia en lugar de dejar que falle la siguiente búsqueda.
      orden: !clave && actuales.orden === 'PRECIO' ? 'DISTANCIA' : actuales.orden,
    }));
  }

  protected cambiarRadio(radioKm: number): void {
    this.cambiarFiltros((actuales) => ({ ...actuales, radioKm }));
  }

  protected cambiarOrden(orden: OrdenBusqueda): void {
    this.cambiarFiltros((actuales) => ({ ...actuales, orden }));
  }

  /**
   * Cualquier cambio de filtro devuelve a la primera página.
   *
   * Sin esto, alguien que esté en la página 4 y amplíe el radio se quedaría mirando la
   * página 4 de unos resultados nuevos, que no tiene nada que ver con lo que acaba de
   * pedir; y si el nuevo resultado tuviera menos páginas, vería una lista vacía.
   */
  private cambiarFiltros(cambio: (actuales: FiltrosBusqueda) => FiltrosBusqueda): void {
    this.filtrosStore.actualizar(cambio);
    this.irAPagina(0);
  }

  /** Busca desde un municipio o código postal escrito a mano. */
  protected buscarEnLugar(lugar: Lugar): void {
    this.posicion.set({ latitud: lugar.latitud, longitud: lugar.longitud });
    this.ubicacionReal.set(false);
    this.lugarElegido.set(`${lugar.nombre} (${lugar.detalle})`);
    this.aviso.set(null);
    this.irAPagina(0);
  }

  protected irAPagina(pagina: number): void {
    this.pagina.set(pagina);
    // La estación resaltada era de la página anterior: ya no está en la lista.
    this.seleccionada.set(null);

    // Como la lista tiene su propio scroll, cambiar de página sin esto te deja a mitad
    // de los resultados nuevos, mirando la séptima gasolinera en vez de la primera.
    this.listaScroll()?.nativeElement.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected seleccionar(estacion: EstacionCercana): void {
    this.seleccionada.set(estacion.id === this.seleccionada() ? null : estacion.id);
  }

  /**
   * Marca o desmarca un favorito.
   *
   * Sin sesión no se puede guardar nada, así que se manda a entrar en lugar de dejar
   * que el servidor conteste 401 y no ocurra nada visible.
   */
  protected alternarFavorito(estacion: EstacionCercana): void {
    if (!this.autenticado()) {
      void this.router.navigate(['/']);
      return;
    }

    const operacion = this.favoritosService.esFavorita(estacion.id)
      ? this.favoritosService.quitar(estacion.id)
      : this.favoritosService.anadir(estacion.id);

    operacion.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: () => this.aviso.set('No hemos podido guardar el favorito. Inténtalo otra vez.'),
    });
  }

  protected mensajeDeError(error: unknown): string {
    // El backend responde 503 mientras no ha podido descargar los datos del Ministerio.
    if (typeof error === 'object' && error !== null && 'status' in error && error.status === 503) {
      return 'Los precios del Ministerio todavía se están cargando. Prueba dentro de un minuto.';
    }
    return 'No hemos podido buscar gasolineras. Comprueba tu conexión e inténtalo otra vez.';
  }
}
