import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { Lugar } from '../core/api.model';
import { EstacionesService } from '../core/estaciones.service';

/**
 * Caja para buscar por municipio o código postal.
 *
 * Es la alternativa a la geolocalización: sin permiso del navegador (o simplemente para
 * mirar precios en otro sitio antes de un viaje), se escribe "Getafe" o "28001".
 *
 * Solo encuentra municipios y códigos postales, no direcciones concretas: las
 * sugerencias salen de los datos del Ministerio, no de un callejero (ver Lugar.java en
 * el backend).
 */
@Component({
  selector: 'app-buscador-lugar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="caja">
      <label [for]="campo()">{{ etiqueta() }}</label>

      <div class="entrada">
        <input
          [id]="campo()"
          type="search"
          [value]="texto()"
          (input)="escribir($any($event.target).value)"
          (keydown.escape)="cerrar()"
          [placeholder]="pista()"
          autocomplete="off"
          role="combobox"
          [attr.aria-controls]="campo() + '-sugerencias'"
          [attr.aria-expanded]="hayResultados()"
        />

        @if (texto()) {
          <button type="button" class="limpiar" (click)="limpiar()" aria-label="Borrar la búsqueda">
            ✕
          </button>
        }
      </div>

      @if (abierto()) {
        <!-- Mientras se teclea (los 250 ms de espera) también es «Buscando…»: si no, salía
             «No hemos encontrado…» con lo que aún no se había preguntado. -->
        @if (sugerencias.isLoading() || esperando()) {
          <p class="mensaje" role="status">Buscando…</p>
        } @else if (sugerencias.value(); as lugares) {
          @if (lugares.length > 0) {
            <ul class="sugerencias" [id]="campo() + '-sugerencias'" role="listbox">
              @for (lugar of lugares; track lugar.tipo + lugar.nombre + lugar.detalle) {
                <li role="option" [attr.aria-selected]="false">
                  <button type="button" (click)="elegir(lugar)">
                    <span class="sugerencia__icono" aria-hidden="true">
                      {{ lugar.tipo === 'CODIGO_POSTAL' ? '#' : '📍' }}
                    </span>
                    <span class="sugerencia__texto">
                      <span class="sugerencia__nombre">{{ lugar.nombre }}</span>
                      <span class="sugerencia__detalle">{{ lugar.detalle }}</span>
                    </span>
                    <span class="sugerencia__cuenta">
                      {{ lugar.estaciones }}
                      {{ lugar.estaciones === 1 ? 'gasolinera' : 'gasolineras' }}
                    </span>
                  </button>
                </li>
              }
            </ul>
          } @else if (texto().length >= 2) {
            <p class="mensaje" role="status">
              No hemos encontrado ningún sitio con ese nombre. Prueba con el municipio o el
              código postal.
            </p>
          }
        }
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      position: relative;
    }

    .caja {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    label {
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--tinta-suave);
    }

    .entrada {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      min-height: 44px;
      padding-inline: 8px 4px;
      border: 2px solid var(--placa-nombre-lila);
      border-radius: 999px;
      background: var(--papel);

      &:focus-within {
        border-color: var(--rosa-boton);
      }
    }

    input {
      flex: 1;
      min-inline-size: 0;
      padding: 0.55rem 0.7rem;
      border: none;
      background: none;
      color: var(--tinta);
      font: inherit;

      &:focus-visible {
        outline: none;
      }
    }

    .limpiar {
      padding: 0 0.6rem;
      border: none;
      background: none;
      color: var(--tinta-suave);
      font-size: 0.9rem;
      cursor: pointer;
    }

    .sugerencias {
      list-style: none;
      margin: 0.35rem 0 0;
      padding: 0.25rem;
      position: absolute;
      inset-inline: 0;
      inset-block-start: 100%;
      z-index: 20;
      background: var(--superficie);
      border: 1px solid var(--borde);
      border-radius: 0.5rem;
      box-shadow: 0 6px 20px rgb(0 0 0 / 15%);
      max-block-size: 20rem;
      overflow-y: auto;

      button {
        inline-size: 100%;
        display: flex;
        align-items: center;
        gap: 0.6rem;
        padding: 0.5rem 0.6rem;
        border: none;
        border-radius: 0.35rem;
        background: none;
        color: inherit;
        font: inherit;
        text-align: start;
        cursor: pointer;

        &:hover,
        &:focus-visible {
          background: color-mix(in srgb, var(--azul) 12%, transparent);
        }
      }
    }

    .sugerencia__icono {
      color: var(--tinta-suave);
    }

    .sugerencia__texto {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-inline-size: 0;
    }

    .sugerencia__nombre {
      font-weight: 600;
    }

    .sugerencia__detalle {
      font-size: 0.8rem;
      color: var(--tinta-suave);
    }

    .sugerencia__cuenta {
      font-size: 0.75rem;
      color: var(--tinta-suave);
      white-space: nowrap;
    }

    .mensaje {
      margin: 0.35rem 0 0;
      padding: 0.6rem 0.7rem;
      position: absolute;
      inset-inline: 0;
      inset-block-start: 100%;
      z-index: 20;
      background: var(--superficie);
      border: 1px solid var(--borde);
      border-radius: 0.5rem;
      font-size: 0.85rem;
      color: var(--tinta-suave);
    }
  `,
})
export class BuscadorLugar {
  private readonly estaciones = inject(EstacionesService);

  /** Viajes pone dos en la misma página (origen y destino): cada uno con su id. */
  readonly campo = input('lugar');
  readonly etiqueta = input('Buscar por municipio o código postal');
  readonly pista = input('Getafe, 28001…');

  readonly lugarElegido = output<Lugar>();

  protected readonly texto = signal('');
  protected readonly abierto = signal(false);

  /**
   * Lo escrito, pero esperando a que se deje de teclear.
   *
   * El debounce va aquí, sobre el PARÁMETRO, y no dentro del stream: un `debounceTime`
   * sobre la petición HTTP no ahorraría ninguna llamada, porque el observable se suscribe
   * igual y lo único que se retrasaría es la respuesta. Debounceando la entrada, escribir
   * "Alcalá de Henares" hace una petición y no diecisiete.
   */
  private readonly textoBuscado = toSignal(
    toObservable(this.texto).pipe(debounceTime(250), distinctUntilChanged()),
    { initialValue: '' },
  );

  /**
   * Sugerencias del backend.
   *
   * `rxResource` cancela la anterior cuando entra una nueva, así que dos respuestas no
   * pueden llegar desordenadas y pisar la buena con una vieja.
   */
  protected readonly sugerencias = rxResource({
    params: () => ({ texto: this.textoBuscado() }),
    stream: ({ params }) => {
      if (params.texto.trim().length < 2) {
        return of<Lugar[]>([]);
      }
      return this.estaciones.lugares(params.texto).pipe(catchError(() => of<Lugar[]>([])));
    },
  });

  /** Lo escrito aún no se ha preguntado: el debounce no ha soltado el texto nuevo. */
  protected esperando(): boolean {
    return this.texto().trim().length >= 2 && this.texto() !== this.textoBuscado();
  }

  protected hayResultados(): boolean {
    return this.abierto() && (this.sugerencias.value()?.length ?? 0) > 0;
  }

  protected escribir(valor: string): void {
    this.texto.set(valor);
    this.abierto.set(true);
  }

  protected elegir(lugar: Lugar): void {
    // Se deja escrito el sitio elegido para que quede claro desde dónde se está buscando.
    this.texto.set(lugar.nombre);
    this.cerrar();
    this.lugarElegido.emit(lugar);
  }

  /** Escribe algo sin buscar (Viajes: «Mi ubicación» al usar la del móvil). */
  poner(texto: string): void {
    this.texto.set(texto);
    this.cerrar();
  }

  protected limpiar(): void {
    this.texto.set('');
    this.cerrar();
  }

  protected cerrar(): void {
    this.abierto.set(false);
  }
}
