import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IonContent, IonFooter } from '@ionic/angular';
import { Observable } from 'rxjs';

import { mensajeDeError } from '../core/api';
import type { GastoDeTicket } from '../gasto/anotar/anotar-gasto.page';
import { HogaresService } from '../hogar/hogares.service';
import { ListaService } from '../lista/lista.service';
import { leerFoto } from './ocr';
import { Borrador, Cadena, Guardado, NOMBRE_CADENA, Unidad } from './ticket.model';
import { TicketsService } from './tickets.service';

/** Una línea mientras se repasa. `casado` null = sin casar. */
interface LineaEditable {
  clave: number;
  texto: string;
  cantidad: number;
  unidad: Unidad;
  precioUnitario: number | null;
  importe: number;
  casado: { ean: string | null; nombre: string; parecido: boolean } | null;
  recienCasada: boolean;
}

type Paso =
  | { tipo: 'eligiendo' }
  | { tipo: 'leyendo'; que: 'pdf' | 'foto'; avance: number }
  | { tipo: 'repasando' }
  | { tipo: 'guardado'; resultado: Guardado };

/**
 * Subir un ticket: PDF (ticket digital) o foto. Se lee, se repasa en la tira (casar cada
 * línea, corregir importes, quitar lo que no es) y se guarda.
 */
@Component({
  selector: 'app-nuevo-ticket',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterLink],
  templateUrl: './nuevo-ticket.page.html',
  styleUrls: ['./tira.scss', './nuevo-ticket.page.scss'],
})
export class NuevoTicketPage {
  private readonly tickets = inject(TicketsService);
  private readonly hogares = inject(HogaresService);
  private readonly lista = inject(ListaService);
  private readonly router = inject(Router);

  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly cadenas = Object.keys(NOMBRE_CADENA) as Cadena[];

  protected readonly paso = signal<Paso>({ tipo: 'eligiendo' });
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal(false);

  protected readonly cadena = signal<Cadena>('OTRA');
  protected readonly tiendaTexto = signal<string | null>(null);
  protected readonly fecha = signal(hoy());
  protected readonly totalLeido = signal<number | null>(null);
  protected readonly origen = signal<'PDF' | 'FOTO'>('PDF');
  protected readonly avisos = signal<string[]>([]);
  protected readonly lineas = signal<LineaEditable[]>([]);
  protected readonly abierta = signal<number | null>(null);
  protected readonly busqueda = signal('');

  protected readonly suma = computed(() => redondear(this.lineas().reduce((t, l) => t + l.importe, 0)));
  protected readonly sinCasar = computed(() => this.lineas().filter((l) => !l.casado).length);
  /** Si el total leído no cuadra con lo que queda en la tira, se dice. */
  protected readonly descuadre = computed(() => {
    const total = this.totalLeido();
    return total !== null && Math.abs(total - this.suma()) > 0.02 ? total : null;
  });
  /** Para casar: lo de la lista del hogar, lo pendiente primero, filtrado por lo escrito. */
  protected readonly candidatos = computed(() => {
    const filtro = this.busqueda().trim().toLocaleLowerCase('es');
    const vistos = new Set<string>();
    return [...this.lista.pendientes(), ...this.lista.enElCarro()]
      .filter((a) => {
        const clave = a.nombre.toLocaleLowerCase('es');
        if (vistos.has(clave)) return false;
        vistos.add(clave);
        return !filtro || clave.includes(filtro);
      })
      .slice(0, 8);
  });

  private readonly hogarId = this.hogares.activo()?.id ?? '';

  constructor() {
    if (this.hogarId) this.lista.abrir(this.hogarId);
  }

  protected subirPdf(evento: Event): void {
    const archivo = elegido(evento);
    if (!archivo) return;
    this.paso.set({ tipo: 'leyendo', que: 'pdf', avance: 0 });
    this.leer(this.tickets.leerPdf(this.hogarId, archivo));
  }

  protected async subirFoto(evento: Event): Promise<void> {
    const archivo = elegido(evento);
    if (!archivo) return;
    this.paso.set({ tipo: 'leyendo', que: 'foto', avance: 0 });
    this.error.set(null);
    try {
      const texto = await leerFoto(archivo, (avance) => this.paso.set({ tipo: 'leyendo', que: 'foto', avance }));
      this.leer(this.tickets.leerTexto(this.hogarId, texto));
    } catch {
      this.paso.set({ tipo: 'eligiendo' });
      this.error.set('No hemos podido leer la foto. Prueba con más luz y el ticket estirado.');
    }
  }

  protected abrir(linea: LineaEditable): void {
    this.abierta.set(this.abierta() === linea.clave ? null : linea.clave);
    this.busqueda.set('');
  }

  protected casar(linea: LineaEditable, ean: string | null, nombre: string): void {
    const limpio = nombre.trim();
    if (!limpio) return;
    this.cambiar(linea.clave, { casado: { ean, nombre: limpio, parecido: false }, recienCasada: true });
    this.abierta.set(null);
  }

  protected descasar(linea: LineaEditable): void {
    this.cambiar(linea.clave, { casado: null });
  }

  protected quitar(linea: LineaEditable): void {
    this.lineas.update((ls) => ls.filter((l) => l.clave !== linea.clave));
    this.abierta.set(null);
  }

  protected cambiarImporte(linea: LineaEditable, evento: Event): void {
    const valor = Number(texto(evento).replace(',', '.'));
    if (Number.isFinite(valor)) this.cambiar(linea.clave, { importe: redondear(valor) });
  }

  protected elegirCadena(evento: Event): void {
    const valor = texto(evento);
    const cadena = this.cadenas.find((c) => c === valor);
    if (cadena) this.cadena.set(cadena);
  }

  protected escribir(evento: Event): string {
    return texto(evento);
  }

  protected euros(valor: number): string {
    return valor.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  protected detalle(l: LineaEditable): string | null {
    if (l.unidad !== 'UD') {
      const u = l.unidad === 'KG' ? 'kg' : 'L';
      return `${l.cantidad.toLocaleString('es-ES')} ${u}${l.precioUnitario ? ` × ${this.euros(l.precioUnitario)} €/${u}` : ''}`;
    }
    return l.cantidad > 1 && l.precioUnitario ? `${l.cantidad} × ${this.euros(l.precioUnitario)}` : null;
  }

  protected guardar(): void {
    const lineas = this.lineas();
    if (lineas.length === 0) {
      this.error.set('El ticket no tiene ninguna línea.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.tickets
      .guardar(this.hogarId, {
        cadena: this.cadena(),
        tiendaTexto: this.tiendaTexto(),
        fecha: this.fecha(),
        total: this.descuadre() === null ? this.totalLeido() : null,
        origen: this.origen(),
        lineas: lineas.map((l) => ({
          texto: l.texto,
          cantidad: l.cantidad,
          unidad: l.unidad,
          precioUnitario: l.precioUnitario,
          importe: l.importe,
          ean: l.casado?.ean ?? null,
          nombre: l.casado?.nombre ?? null,
        })),
      })
      .subscribe({
        next: (resultado) => {
          this.guardando.set(false);
          this.paso.set({ tipo: 'guardado', resultado });
          this.lista.refrescar();
        },
        error: (e: unknown) => {
          this.guardando.set(false);
          this.error.set(mensajeDeError(e));
        },
      });
  }

  protected otra(): void {
    this.paso.set({ tipo: 'eligiendo' });
    this.lineas.set([]);
    this.error.set(null);
  }

  protected verTicket(id: string): void {
    void this.router.navigate(['/tickets', id], { replaceUrl: true });
  }

  private leer(peticion: Observable<Borrador>): void {
    this.error.set(null);
    peticion.subscribe({
      next: (b) => this.repasar(b),
      error: (e: unknown) => {
        this.paso.set({ tipo: 'eligiendo' });
        this.error.set(mensajeDeError(e, 'No hemos podido leer el ticket.'));
      },
    });
  }

  /** El ticket no es del súper: a Anotar un gasto, con la tienda, el total y la fecha ya puestos. */
  protected comoGasto(): void {
    const deTicket: GastoDeTicket = {
      concepto: this.tiendaTexto() ?? '',
      importe: this.totalLeido() ?? (this.suma() > 0 ? this.suma() : null),
      fecha: this.fecha(),
    };
    void this.router.navigate(['/gastos/nuevo'], { state: { deTicket } });
  }

  private repasar(b: Borrador): void {
    this.cadena.set(b.cadena);
    this.tiendaTexto.set(b.tiendaTexto);
    this.fecha.set(b.fecha ?? hoy());
    this.totalLeido.set(b.total);
    this.origen.set(b.origen);
    this.avisos.set(b.avisos.filter((a) => !a.startsWith('Las líneas suman')));
    this.lineas.set(
      b.lineas.map((l, i) => ({
        clave: i,
        texto: l.texto,
        cantidad: l.cantidad,
        unidad: l.unidad,
        precioUnitario: l.precioUnitario,
        importe: l.importe,
        casado: l.sugerencia
          ? { ean: l.sugerencia.ean, nombre: l.sugerencia.nombre, parecido: l.sugerencia.origen === 'PARECIDO' }
          : null,
        recienCasada: false,
      })),
    );
    this.paso.set({ tipo: 'repasando' });
  }

  private cambiar(clave: number, cambio: Partial<LineaEditable>): void {
    this.lineas.update((ls) => ls.map((l) => (l.clave === clave ? { ...l, ...cambio } : l)));
  }
}

function elegido(evento: Event): File | null {
  const entrada = evento.target instanceof HTMLInputElement ? evento.target : null;
  const archivo = entrada?.files?.[0] ?? null;
  if (entrada) entrada.value = '';
  return archivo;
}

function texto(evento: Event): string {
  return evento.target instanceof HTMLInputElement || evento.target instanceof HTMLSelectElement
    ? evento.target.value
    : '';
}

function redondear(valor: number): number {
  return Math.round(valor * 100) / 100;
}

function hoy(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
