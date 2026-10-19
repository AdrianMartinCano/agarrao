import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IonContent, NavController } from '@ionic/angular';

import { mensajeDeError } from '../../core/api';
import { HogaresService } from '../../hogar/hogares.service';
import { leerFoto } from '../../tickets/ocr';
import { Borrador } from '../../tickets/ticket.model';
import { TicketsService } from '../../tickets/tickets.service';
import { GastoAnotado, GastoService, NOMBRE_TIPO, TipoGasto, TIPOS_GASTO } from '../gasto.service';

/** Lo que llega de «Subir un ticket» cuando el ticket no es del súper. */
export interface GastoDeTicket {
  concepto: string;
  importe: number | null;
  fecha: string | null;
}

/**
 * Anotar un gasto (o cambiar uno ya anotado, con :id). Se puede rellenar leyendo un ticket
 * en PDF o una foto: se cogen la tienda, el total y la fecha.
 */
@Component({
  selector: 'app-anotar-gasto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  templateUrl: './anotar-gasto.page.html',
  styleUrl: './anotar-gasto.page.scss',
})
export class AnotarGastoPage implements OnInit {
  private readonly hogares = inject(HogaresService);
  private readonly gastos = inject(GastoService);
  private readonly tickets = inject(TicketsService);
  private readonly nav = inject(NavController);
  private readonly router = inject(Router);

  /** De la ruta: el gasto a cambiar. Sin id, uno nuevo. */
  readonly id = input<string>();

  protected readonly tipos = TIPOS_GASTO;
  protected readonly nombreTipo = NOMBRE_TIPO;
  protected readonly hoy = hoyIso();

  protected readonly concepto = signal('');
  protected readonly importe = signal('');
  protected readonly fecha = signal(hoyIso());
  protected readonly tipo = signal<TipoGasto | null>(null);
  protected readonly cadaMes = signal(false);

  protected readonly cargando = signal(false);
  protected readonly leyendo = signal<string | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmandoBorrar = signal(false);
  /** El gasto cargado al cambiar uno. */
  protected readonly original = signal<GastoAnotado | null>(null);

  protected readonly editando = computed(() => !!this.id());
  private readonly hogarId = this.hogares.activo()?.id ?? '';
  /** Se lee al construir: después de navegar ya no está. */
  private readonly deTicket = (this.router.getCurrentNavigation()?.extras.state?.['deTicket'] ??
    null) as GastoDeTicket | null;

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      if (this.deTicket) this.rellenar(this.deTicket);
      return;
    }
    this.cargando.set(true);
    this.gastos.uno(this.hogarId, id).subscribe({
      next: (g) => {
        this.cargando.set(false);
        this.original.set(g);
        this.concepto.set(g.concepto);
        this.importe.set(enTexto(g.importe));
        this.fecha.set(g.fecha);
        this.tipo.set(g.tipo);
      },
      error: (e: unknown) => {
        this.cargando.set(false);
        this.error.set(mensajeDeError(e, 'No se ha podido cargar ese gasto.'));
      },
    });
  }

  protected valor(evento: Event): string {
    return (evento.target as HTMLInputElement).value;
  }

  protected leerPdf(evento: Event): void {
    const archivo = elegido(evento);
    if (!archivo) return;
    this.leyendo.set('Leyendo el PDF…');
    this.error.set(null);
    this.tickets.leerPdf(this.hogarId, archivo).subscribe({
      next: (b) => this.desdeBorrador(b),
      error: (e: unknown) => {
        this.leyendo.set(null);
        this.error.set(mensajeDeError(e, 'No hemos podido leer ese PDF.'));
      },
    });
  }

  protected async leerFoto(evento: Event): Promise<void> {
    const archivo = elegido(evento);
    if (!archivo) return;
    this.error.set(null);
    this.leyendo.set('Leyendo la foto…');
    try {
      const texto = await leerFoto(archivo, (avance) =>
        this.leyendo.set(`Leyendo la foto… ${Math.round(avance * 100)} %`),
      );
      this.tickets.leerTexto(this.hogarId, texto).subscribe({
        next: (b) => this.desdeBorrador(b),
        error: (e: unknown) => {
          this.leyendo.set(null);
          this.error.set(mensajeDeError(e, 'No hemos podido leer la foto.'));
        },
      });
    } catch {
      this.leyendo.set(null);
      this.error.set('No hemos podido leer la foto. Prueba con más luz y el ticket estirado.');
    }
  }

  protected guardar(evento: Event): void {
    evento.preventDefault();
    const concepto = this.concepto().trim();
    const importe = Number(this.importe().replace(',', '.'));
    const tipo = this.tipo();
    if (!concepto) return this.error.set('Escribe en qué ha sido.');
    if (!Number.isFinite(importe) || importe <= 0) return this.error.set('Pon cuánto ha sido, por ejemplo 12,50.');
    if (!tipo) return this.error.set('Elige de qué tipo es.');
    if (!this.fecha() || this.fecha() > this.hoy) return this.error.set('La fecha no puede ser futura.');

    const gasto = {
      concepto,
      importe: Math.round(importe * 100) / 100,
      fecha: this.fecha(),
      tipo,
      cadaMes: !this.editando() && this.cadaMes(),
    };
    const id = this.id();
    this.guardando.set(true);
    this.error.set(null);
    (id ? this.gastos.cambiar(this.hogarId, id, gasto) : this.gastos.anotar(this.hogarId, gasto)).subscribe({
      next: () => this.volver(gasto.fecha),
      error: (e: unknown) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se ha guardado. Revisa la conexión.'));
      },
    });
  }

  protected borrar(): void {
    const id = this.id();
    if (!id) return;
    if (!this.confirmandoBorrar()) {
      this.confirmandoBorrar.set(true);
      return;
    }
    this.guardando.set(true);
    this.gastos.borrar(this.hogarId, id).subscribe({
      next: () => this.volver(this.fecha()),
      error: (e: unknown) => {
        this.guardando.set(false);
        this.error.set(mensajeDeError(e, 'No se ha podido borrar.'));
      },
    });
  }

  /** A Gasto, en el mes del gasto. */
  private volver(fecha: string): void {
    void this.nav.navigateRoot('/gasto', { queryParams: { mes: fecha.slice(0, 7) } });
  }

  private desdeBorrador(b: Borrador): void {
    this.leyendo.set(null);
    this.rellenar({ concepto: b.tiendaTexto ?? '', importe: b.total, fecha: b.fecha });
    if (b.total === null) this.error.set('No hemos encontrado el total: escríbelo tú.');
  }

  private rellenar(d: GastoDeTicket): void {
    if (d.concepto && !this.concepto()) this.concepto.set(nombreDeTicket(d.concepto));
    if (d.importe !== null) this.importe.set(enTexto(d.importe));
    if (d.fecha && d.fecha <= this.hoy) this.fecha.set(d.fecha);
  }
}

function hoyIso(): string {
  const h = new Date();
  return `${h.getFullYear()}-${String(h.getMonth() + 1).padStart(2, '0')}-${String(h.getDate()).padStart(2, '0')}`;
}

function enTexto(valor: number): string {
  return valor.toFixed(2).replace('.', ',');
}

/** «RESTAURANTE LA TAGLIATELLA» → «Restaurante La Tagliatella» (los tickets van en mayúsculas). */
export function nombreDeTicket(texto: string): string {
  const limpio = texto.trim().slice(0, 80);
  if (limpio !== limpio.toUpperCase()) return limpio;
  return limpio
    .toLocaleLowerCase('es')
    .replace(/(^|\s)(\p{L})/gu, (_, espacio: string, letra: string) => espacio + letra.toLocaleUpperCase('es'));
}

function elegido(evento: Event): File | null {
  const campo = evento.target as HTMLInputElement;
  const archivo = campo.files?.[0] ?? null;
  campo.value = '';
  return archivo;
}
