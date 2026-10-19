import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { EstacionCercana } from '../core/api.model';
import { CARBURANTES_DESTACADOS } from '../core/como-llegar';
import { FavoritosService } from '../core/favoritos.service';
import { GeolocalizacionService } from '../core/geolocalizacion.service';
import { ListaEstaciones } from '../buscador/lista-estaciones';

/**
 * Las gasolineras guardadas por el usuario, con el precio de ahora mismo.
 *
 * La ubicación es opcional aquí: los favoritos se ven igual sin dar permiso, solo que
 * sin la distancia. Por eso el error de geolocalización se ignora en silencio en lugar
 * de enseñar una alarma; en esta pantalla no impide hacer nada.
 */
@Component({
  selector: 'app-favoritos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ListaEstaciones, RouterLink],
  template: `
    <header class="cabecera">
      <h1>Mis favoritas</h1>
      <p class="intro">Con los precios más recientes que publica el Ministerio.</p>
    </header>

    @if (favoritos.isLoading()) {
      <p class="estado" role="status">Cargando tus gasolineras…</p>
    } @else if (favoritos.error()) {
      <div class="estado estado--error" role="alert">
        <p>No hemos podido cargar tus favoritas.</p>
        <button type="button" (click)="favoritos.reload()">Reintentar</button>
      </div>
    } @else if (favoritos.value(); as estaciones) {
      @if (estaciones.length === 0) {
        <div class="vacio">
          <p>Todavía no has guardado ninguna gasolinera.</p>
          <a routerLink="/gasolina" class="enlace-boton">Buscar gasolineras</a>
        </div>
      } @else {
        <app-lista-estaciones
          [estaciones]="estaciones"
          [favoritos]="idsFavoritos()"
          [mostrarFavoritos]="true"
          [preciosDestacados]="carburantesDestacados"
          (favoritoAlternado)="quitar($event)"
        />
      }
    }
  `,
  styles: `
    :host {
      display: block;
      max-inline-size: 45rem;
      margin-inline: auto;
    }

    .cabecera {
      margin-block-end: 1.5rem;
    }

    h1 {
      margin: 0;
      font-size: 1.5rem;
    }

    .intro {
      margin: 0.25rem 0 0;
      color: var(--tinta-suave);
      font-size: 0.9rem;
    }

    .estado {
      padding: 2rem 1rem;
      text-align: center;
      color: var(--tinta-suave);
    }

    .estado--error {
      border: 1px solid #ef4444;
      border-radius: 0.75rem;
      background: color-mix(in srgb, #ef4444 8%, transparent);
      color: var(--tinta);

      button {
        margin-block-start: 0.75rem;
        padding: 0.5rem 1rem;
        border: 1px solid var(--borde);
        border-radius: 0.5rem;
        background: var(--fondo);
        color: var(--tinta);
        font: inherit;
        cursor: pointer;
      }
    }

    .vacio {
      padding: 3rem 1rem;
      text-align: center;
      color: var(--tinta-suave);
    }

    /* El botón de Agarrao: píldora rosa */
    .enlace-boton {
      display: inline-flex;
      align-items: center;
      min-height: 48px;
      margin-block-start: 1rem;
      padding: 0 22px;
      border-radius: 999px;
      background: var(--rosa-boton);
      color: #fff;
      text-decoration: none;
      font-weight: 800;
    }
  `,
})
export class FavoritosPage {
  private readonly favoritosService = inject(FavoritosService);
  private readonly geolocalizacion = inject(GeolocalizacionService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly idsFavoritos = this.favoritosService.idsFavoritos;

  // En esta pantalla no hay filtro de carburante, así que cada tarjeta enseña los dos
  // precios de siempre. Son los mismos que salen en el globo del mapa.
  protected readonly carburantesDestacados = CARBURANTES_DESTACADOS;

  private readonly posicion = signal<{ latitud: number; longitud: number } | null>(null);

  protected readonly favoritos = rxResource({
    params: () => ({ posicion: this.posicion() }),
    stream: ({ params }) => this.favoritosService.listar(params.posicion, null),
  });

  constructor() {
    this.geolocalizacion
      .posicionActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (posicion) => this.posicion.set(posicion),
        // Sin ubicación se ven igual, solo que sin distancia: no hay nada que avisar.
        error: () => undefined,
      });
  }

  protected quitar(estacion: EstacionCercana): void {
    this.favoritosService
      .quitar(estacion.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      // Se recarga en vez de quitarlo de la lista a mano: así lo que se ve siempre es
      // lo que hay guardado de verdad.
      .subscribe({ next: () => this.favoritos.reload() });
  }
}
