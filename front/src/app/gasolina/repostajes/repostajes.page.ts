import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { EstacionCercana } from '../core/api.model';
import { RepostajesService } from '../core/repostajes.service';
import { Repostaje, Vehiculo } from '../core/repostajes.model';
import { mensajeDeErrorHttp } from '../core/mensaje-error';
import { Graficas } from './graficas';
import { SelectorGasolinera } from './selector-gasolinera';

/**
 * Registrar repostajes y ver el historial.
 *
 * EL FORMULARIO SE RELLENA SOLO en lo posible: al elegir la gasolinera entra su precio de
 * hoy, y con el precio puesto basta con escribir lo que se pagó para que salgan los
 * litros. Registrar un repostaje debería costar menos que guardar el ticket.
 *
 * Importe y litros están enlazados en los DOS sentidos: quien mire el surtidor escribirá
 * los litros, y quien mire el ticket escribirá los euros. Los dos caminos valen.
 */
@Component({
  selector: 'app-repostajes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, Graficas, ReactiveFormsModule, RouterLink, SelectorGasolinera],
  templateUrl: './repostajes.page.html',
  styleUrl: './repostajes.page.scss',
})
export class RepostajesPage {
  private readonly fb = inject(FormBuilder);
  private readonly servicio = inject(RepostajesService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly formularioAbierto = signal(false);
  protected readonly estacion = signal<EstacionCercana | null>(null);

  /** Historial y gráficas comparten página: los datos son los mismos vistos de dos formas. */
  protected readonly pestana = signal<'historial' | 'graficas'>('historial');

  /** El repostaje que se está corrigiendo, o null si el formulario es para uno nuevo. */
  protected readonly editando = signal<Repostaje | null>(null);

  protected readonly vehiculos = rxResource({
    params: () => ({}),
    stream: () => this.servicio.vehiculos().pipe(catchError(() => of<Vehiculo[]>([]))),
  });

  protected readonly historial = rxResource({
    params: () => ({}),
    stream: () => this.servicio.repostajes().pipe(catchError(() => of([]))),
  });

  protected readonly formulario = this.fb.nonNullable.group({
    vehiculoId: ['', [Validators.required]],
    fecha: [hoy(), [Validators.required]],
    litros: [null as number | null, [Validators.required, Validators.min(0.01)]],
    precioLitro: [null as number | null, [Validators.required, Validators.min(0.01)]],
    importe: [null as number | null],
    kilometros: [null as number | null],
    depositoLleno: [true],
    notas: [''],
  });

  /** El vehículo elegido, para saber qué carburante buscar y preseleccionar. */
  protected readonly vehiculoElegido = computed(() => {
    const id = this.idVehiculo();
    return this.vehiculos.value()?.find((v) => v.id === id) ?? null;
  });

  private readonly idVehiculo = signal('');

  constructor() {
    // Cuando llegan los vehículos, se elige el primero: con un solo coche —el caso
    // normal— no hay que tocar el desplegable.
    this.formulario.controls.vehiculoId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((id) => this.idVehiculo.set(id));
  }

  protected abrirFormulario(): void {
    const vehiculos = this.vehiculos.value() ?? [];
    if (vehiculos.length > 0 && !this.formulario.controls.vehiculoId.value) {
      this.formulario.controls.vehiculoId.setValue(vehiculos[0].id);
    }
    this.editando.set(null);
    this.formularioAbierto.set(true);
  }

  /**
   * Carga un repostaje en el formulario para corregirlo.
   *
   * La gasolinera NO se puede precargar en el selector: lo que se guardó es su nombre y
   * su id, y el selector trabaja con estaciones completas del catálogo. Se avisa en la
   * interfaz de que, si no se toca, se conserva la que ya tenía.
   */
  protected editar(repostaje: Repostaje): void {
    this.editando.set(repostaje);
    this.error.set(null);
    this.estacion.set(null);
    this.formulario.setValue({
      vehiculoId: repostaje.vehiculoId,
      fecha: repostaje.fecha.slice(0, 10),
      litros: repostaje.litros,
      precioLitro: repostaje.precioLitro,
      importe: repostaje.importe,
      kilometros: repostaje.kilometros,
      depositoLleno: repostaje.depositoLleno,
      notas: repostaje.notas ?? '',
    });
    this.formularioAbierto.set(true);
  }

  protected cerrarFormulario(): void {
    this.formularioAbierto.set(false);
    this.editando.set(null);
    this.error.set(null);
  }

  /**
   * Al elegir gasolinera entra su precio, y con él se recalculan litros o importe.
   *
   * SALVO AL CORREGIR uno ya apuntado: el precio que se pagó es el de aquel día, y el
   * catálogo solo sabe el de hoy. Antes se machacaba, y al cambiar la gasolinera de un
   * repostaje viejo se perdía lo que costó de verdad. Si hay que cambiarlo, se escribe a mano.
   */
  protected gasolineraElegida(estacion: EstacionCercana | null): void {
    this.estacion.set(estacion);

    if (estacion?.precio != null && !this.editando()) {
      this.formulario.controls.precioLitro.setValue(estacion.precio);
      this.recalcularDesdeImporte();
    }
  }

  /**
   * Escribes los euros del ticket → salen los litros. Con tres decimales, como el
   * surtidor: con dos, el redondeo hacía que 20 € volvieran como 20,01 €. Además el
   * importe se manda tal cual al guardar, así que lo que se escribe es lo que se queda.
   */
  protected recalcularDesdeImporte(): void {
    const { importe, precioLitro } = this.formulario.getRawValue();
    if (!importe || !precioLitro) {
      return;
    }
    this.formulario.controls.litros.setValue(redondear(importe / precioLitro, 3), {
      emitEvent: false,
    });
  }

  /** Escribes los litros del surtidor → sale el importe. */
  protected recalcularDesdeLitros(): void {
    const { litros, precioLitro } = this.formulario.getRawValue();
    if (!litros || !precioLitro) {
      return;
    }
    this.formulario.controls.importe.setValue(redondear(litros * precioLitro, 2), {
      emitEvent: false,
    });
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);

    const valores = this.formulario.getRawValue();
    const enEdicion = this.editando();

    const datos = {
      vehiculoId: valores.vehiculoId,
      fecha: valores.fecha,
      // El carburante es el del vehículo: nadie echa gasóleo en un coche de gasolina.
      carburante: this.vehiculoElegido()?.carburante ?? '',
      litros: valores.litros!,
      precioLitro: valores.precioLitro!,
      importe: valores.importe || null,
      kilometros: valores.kilometros,
      depositoLleno: valores.depositoLleno,
      // Al editar sin tocar el selector se conserva la gasolinera que ya tenía; si se
      // elige otra, manda la nueva.
      estacionId: this.estacion()?.id ?? enEdicion?.estacionId ?? null,
      notas: valores.notas || null,
    };

    (enEdicion
      ? this.servicio.actualizarRepostaje(enEdicion.id, datos)
      : this.servicio.crearRepostaje(datos)
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarFormulario();
          this.reiniciarFormulario();
          // Se recargan los dos: el historial cambia y el consumo del vehículo también.
          this.historial.reload();
          this.vehiculos.reload();
        },
        error: (error: unknown) => {
          this.guardando.set(false);
          this.error.set(mensajeDeErrorHttp(error, 'No hemos podido guardar el repostaje.'));
        },
      });
  }

  protected eliminar(id: string): void {
    this.servicio
      .eliminarRepostaje(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.historial.reload();
          this.vehiculos.reload();
        },
      });
  }

  private reiniciarFormulario(): void {
    const vehiculoId = this.formulario.controls.vehiculoId.value;
    this.formulario.reset({ vehiculoId, fecha: hoy(), depositoLleno: true, notas: '' });
    this.estacion.set(null);
  }
}

function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

function redondear(valor: number, decimales: number): number {
  const factor = 10 ** decimales;
  return Math.round(valor * factor) / factor;
}
