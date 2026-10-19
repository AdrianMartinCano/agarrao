import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EstacionesService } from '../core/estaciones.service';
import { RepostajesService } from '../core/repostajes.service';
import { Vehiculo } from '../core/repostajes.model';
import { mensajeDeErrorHttp } from '../core/mensaje-error';

/**
 * Los vehículos del usuario.
 *
 * Separar por vehículo no es una comodidad para quien tenga dos coches: los kilómetros
 * salen del cuentakilómetros de cada uno, y mezclarlos haría imposible medir el consumo.
 */
@Component({
  selector: 'app-vehiculos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <header class="cabecera">
      <div>
        <h1>Mis vehículos</h1>
        <p class="intro">Cada coche lleva sus propios kilómetros y su propio consumo.</p>
      </div>
      <a routerLink="/gasolina/repostajes" class="secundario">Ver repostajes</a>
    </header>

    <!-- El mismo formulario sirve para crear y para corregir: son los mismos tres campos
         y las mismas reglas. Duplicarlo solo daría dos sitios donde arreglar cada cosa. -->
    <section class="alta" [class.alta--editando]="editando() !== null">
      @if (editando(); as vehiculo) {
        <p class="editando-aviso">
          Editando <strong>{{ vehiculo.nombre }}</strong>
        </p>
      }

      <form [formGroup]="formulario" (ngSubmit)="guardar()">
        <!-- Los campos van en su propio grid y los mensajes DEBAJO, fuera de él. Dentro
             de una celda, un error que aparece hace crecer esa columna y descuadra la
             fila entera. -->
        <div class="campos">
          <div class="gcampo">
            <label for="nombre">Nombre</label>
            <input
              id="nombre"
              formControlName="nombre"
              placeholder="El Golf"
              [attr.aria-invalid]="nombreInvalido()"
              [attr.aria-describedby]="nombreInvalido() ? 'error-nombre' : null"
            />
          </div>

          <div class="gcampo">
            <label for="carburante">Carburante</label>
            <select id="carburante" formControlName="carburante">
              @for (carburante of carburantes(); track carburante.clave) {
                <option [value]="carburante.clave">{{ carburante.etiqueta }}</option>
              }
            </select>
          </div>

          <div class="gcampo">
            <label for="matricula">Matrícula</label>
            <input id="matricula" formControlName="matricula" placeholder="Opcional" />
          </div>

          <div class="botones">
            @if (editando()) {
              <button type="button" class="secundario" (click)="cancelarEdicion()">
                Cancelar
              </button>
            }
            <button type="submit" class="principal" [disabled]="guardando()">
              {{ guardando() ? 'Guardando…' : editando() ? 'Guardar' : 'Añadir' }}
            </button>
          </div>
        </div>

        @if (nombreInvalido()) {
          <p class="error-campo" id="error-nombre" role="alert">
            Ponle un nombre para reconocerlo.
          </p>
        }

        @if (error(); as mensaje) {
          <p class="error" role="alert">{{ mensaje }}</p>
        }
      </form>
    </section>

    @if (vehiculos.isLoading()) {
      <p class="estado" role="status">Cargando…</p>
    } @else if (vehiculos.value(); as lista) {
      @if (lista.length === 0) {
        <p class="estado">Todavía no has añadido ningún vehículo.</p>
      } @else {
        <ul class="lista">
          @for (vehiculo of lista; track vehiculo.id) {
            <li class="vehiculo">
              <div>
                <p class="vehiculo__nombre">{{ vehiculo.nombre }}</p>
                <p class="vehiculo__detalle">
                  {{ vehiculo.carburanteEtiqueta }}
                  @if (vehiculo.matricula) {
                    · {{ vehiculo.matricula }}
                  }
                  · {{ vehiculo.repostajes }}
                  {{ vehiculo.repostajes === 1 ? 'repostaje' : 'repostajes' }}
                </p>
              </div>

              <div class="acciones">
                <button type="button" class="accion" (click)="editar(vehiculo)">Editar</button>
                <button type="button" class="accion" (click)="archivar(vehiculo)">Archivar</button>
              </div>
            </li>
          }
        </ul>
        <p class="nota">
          Archivar un vehículo lo quita de la lista pero <strong>conserva su historial</strong>.
        </p>
      }
    }
  `,
  styleUrl: './vehiculos.page.scss',
})
export class VehiculosPage {
  private readonly fb = inject(FormBuilder);
  private readonly servicio = inject(RepostajesService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  /** El vehículo que se está corrigiendo, o null si el formulario es para uno nuevo. */
  protected readonly editando = signal<Vehiculo | null>(null);

  /** Los mismos cinco que ofrece el buscador. */
  protected readonly carburantes = toSignal(
    inject(EstacionesService)
      .carburantes()
      .pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  protected readonly vehiculos = rxResource({
    params: () => ({}),
    stream: () => this.servicio.vehiculos().pipe(catchError(() => of<Vehiculo[]>([]))),
  });

  protected readonly formulario = this.fb.nonNullable.group({
    nombre: ['', [Validators.required]],
    carburante: ['GASOLINA_95_E5', [Validators.required]],
    matricula: [''],
  });

  /**
   * Estado del campo nombre como signal.
   *
   * Los FormControl no son signals, así que su estado no dispara la detección de cambios
   * por sí solo en un componente OnPush. Se refleja aquí al pulsar Añadir, que es cuando
   * puede aparecer el error.
   */
  protected readonly nombreInvalido = signal(false);

  /** Carga el vehículo en el formulario para corregirlo. */
  protected editar(vehiculo: Vehiculo): void {
    this.editando.set(vehiculo);
    this.error.set(null);
    this.nombreInvalido.set(false);
    this.formulario.setValue({
      nombre: vehiculo.nombre,
      carburante: vehiculo.carburante,
      matricula: vehiculo.matricula ?? '',
    });
  }

  protected cancelarEdicion(): void {
    this.editando.set(null);
    this.nombreInvalido.set(false);
    this.formulario.reset({ nombre: '', carburante: 'GASOLINA_95_E5', matricula: '' });
  }

  /** Crea o corrige, según haya un vehículo cargado en el formulario. */
  protected guardar(): void {
    this.nombreInvalido.set(this.formulario.controls.nombre.invalid);

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);

    const { nombre, carburante, matricula } = this.formulario.getRawValue();
    const datos = { nombre, carburante, matricula: matricula || null };
    const enEdicion = this.editando();

    const peticion = enEdicion
      ? this.servicio.actualizarVehiculo(enEdicion.id, datos)
      : this.servicio.crearVehiculo(datos);

    peticion.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.nombreInvalido.set(false);
        this.editando.set(null);
        // Al crear se conserva el carburante elegido: quien añade dos coches suele
        // tenerlos parecidos. Al terminar de editar se vuelve al valor de partida.
        this.formulario.reset({
          nombre: '',
          carburante: enEdicion ? 'GASOLINA_95_E5' : carburante,
          matricula: '',
        });
        this.vehiculos.reload();
      },
      error: (error: unknown) => {
        this.guardando.set(false);
        this.error.set(
          mensajeDeErrorHttp(
            error,
            enEdicion
              ? 'No hemos podido guardar los cambios.'
              : 'No hemos podido añadir el vehículo.',
          ),
        );
      },
    });
  }

  protected archivar(vehiculo: Vehiculo): void {
    this.servicio
      .archivarVehiculo(vehiculo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: () => this.vehiculos.reload() });
  }
}
