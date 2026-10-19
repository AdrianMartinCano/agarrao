import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { IonContent } from '@ionic/angular';

import { mensajeDeError } from '../core/api';
import { HogaresService } from '../hogar/hogares.service';
import { ListaService } from '../lista/lista.service';
import { eanValido } from './ean';
import { Lector, crearLector } from './lector';
import { Producto } from './producto.model';
import { ProductosService, nombreParaLista } from './productos.service';

/** Cada cuánto se mira el vídeo. Más seguido no lee mejor y gasta batería. */
const CADA_MS = 180;

type Estado =
  | { tipo: 'arrancando' }
  | { tipo: 'mirando' }
  | { tipo: 'sin-camara'; motivo: string }
  | { tipo: 'buscando'; ean: string }
  | { tipo: 'encontrado'; producto: Producto }
  /** `sinConexion`: OFF no respondió; se apunta, pero no se le pone nombre para todos. */
  | { tipo: 'desconocido'; ean: string; sinConexion: boolean };

/**
 * El escáner: la cámara dentro de la lupa de Listillo. Lee el código, pregunta al back qué
 * es y lo apunta en la lista del hogar abierto. Si no hay cámara (o se niega el permiso),
 * el código se escribe a mano.
 */
@Component({
  selector: 'app-escaner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent],
  templateUrl: './escaner.page.html',
  styleUrl: './escaner.page.scss',
})
export class EscanerPage {
  private readonly productos = inject(ProductosService);
  private readonly lista = inject(ListaService);
  private readonly hogares = inject(HogaresService);
  private readonly router = inject(Router);

  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');

  protected readonly estado = signal<Estado>({ tipo: 'arrancando' });
  protected readonly manual = signal(false);
  protected readonly codigoManual = signal('');
  protected readonly errorManual = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly apuntado = signal<string | null>(null);
  protected readonly enviando = signal(false);
  /** El destello del aro al leer: dura lo suyo aunque el back conteste al momento. */
  protected readonly destello = signal(false);
  /** El error del formulario de producto nuevo, junto a su campo. */
  protected readonly errorNombre = signal<string | null>(null);

  /** La lupa se encoge cuando hay algo que enseñar debajo. */
  protected readonly lupaPequena = computed(() => {
    const tipo = this.estado().tipo;
    return tipo === 'buscando' || tipo === 'encontrado' || tipo === 'desconocido';
  });
  protected readonly hogar = this.hogares.activo;

  private lector: Lector | null = null;
  private flujo: MediaStream | null = null;
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  /** Se acepta un código cuando se lee igual dos veces seguidas: menos lecturas falsas. */
  private ultimaLectura: string | null = null;
  private temporizadorAviso: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    const hogar = this.hogares.activo();
    if (hogar) this.lista.abrir(hogar.id);

    afterNextRender(() => void this.encenderCamara());
    inject(DestroyRef).onDestroy(() => this.apagarCamara());
  }

  protected volver(): void {
    void this.router.navigateByUrl('/lista');
  }

  protected escribirCodigo(evento: Event): void {
    const valor = evento.target instanceof HTMLInputElement ? evento.target.value : '';
    this.codigoManual.set(valor.replace(/\D/g, '').slice(0, 14));
    this.errorManual.set(null);
  }

  protected buscarManual(evento: Event): void {
    evento.preventDefault();
    const ean = this.codigoManual();
    if (!eanValido(ean)) {
      this.errorManual.set('Ese código no cuadra. Son los números de debajo de las barras.');
      return;
    }
    this.buscar(ean);
  }

  protected apuntar(producto: Producto): void {
    this.lista.apuntar(nombreParaLista(producto), 1, producto.ean);
    this.siguiente(nombreParaLista(producto));
  }

  /** Para un código que nadie conoce: se le pone nombre (para todos) y se apunta. */
  protected nombrarYApuntar(evento: Event, ean: string, sinConexion: boolean, campo: HTMLInputElement, marca: string): void {
    evento.preventDefault();
    const datos = { nombre: campo.value.trim(), marca: marca.trim() || undefined };
    if (!datos.nombre) {
      this.errorNombre.set('Escribe qué es para apuntarlo.');
      campo.focus();
      return;
    }
    this.errorNombre.set(null);
    if (sinConexion) {
      // Sin OFF no se sabe si lo conoce: se apunta, pero no se nombra para todos.
      this.lista.apuntar(nombreParaLista({ nombre: datos.nombre, marca: datos.marca ?? null }), 1, ean);
      this.siguiente(datos.nombre);
      return;
    }
    this.enviando.set(true);
    this.productos.nombrar(ean, datos).subscribe({
      next: (producto) => {
        this.enviando.set(false);
        this.apuntar(producto);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.error.set(mensajeDeError(e));
      },
    });
  }

  /** «No es esto» / tras apuntar: la lupa vuelve a mirar. */
  protected otraVez(): void {
    this.error.set(null);
    this.ultimaLectura = null;
    this.codigoManual.set('');
    this.estado.set(this.flujo ? { tipo: 'mirando' } : { tipo: 'sin-camara', motivo: '' });
    this.programar();
  }

  /** La marca en la placa solo si no va ya en el nombre («Nutella» / «400 g»). */
  protected marcaAparte(producto: Producto): string | null {
    const marca = producto.marca;
    return marca && !producto.nombre.toLowerCase().includes(marca.toLowerCase()) ? marca : null;
  }

  private siguiente(nombre: string): void {
    this.apuntado.set(nombre);
    if (this.temporizadorAviso) clearTimeout(this.temporizadorAviso);
    this.temporizadorAviso = setTimeout(() => this.apuntado.set(null), 2200);
    this.manual.set(false);
    this.otraVez();
  }

  private buscar(ean: string): void {
    this.estado.set({ tipo: 'buscando', ean });
    this.error.set(null);
    this.errorNombre.set(null);
    navigator.vibrate?.(60);
    this.destello.set(true);
    setTimeout(() => this.destello.set(false), 600);
    this.productos.buscar(ean).subscribe({
      next: (producto) => this.estado.set({ tipo: 'encontrado', producto }),
      error: (e: unknown) => {
        const estado = e instanceof HttpErrorResponse ? e.status : 0;
        if (estado === 404 || estado === 503 || estado === 0) {
          this.estado.set({ tipo: 'desconocido', ean, sinConexion: estado !== 404 });
        } else {
          this.error.set(mensajeDeError(e));
          this.otraVez();
        }
      },
    });
  }

  private async encenderCamara(): Promise<void> {
    const video = this.video()?.nativeElement;
    if (!video || !navigator.mediaDevices?.getUserMedia) {
      this.sinCamara('Este navegador no deja usar la cámara. Escribe el código.');
      return;
    }
    try {
      this.flujo = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 } },
        audio: false,
      });
    } catch (e) {
      const negado = e instanceof DOMException && (e.name === 'NotAllowedError' || e.name === 'SecurityError');
      this.sinCamara(
        negado
          ? 'Sin permiso para la cámara. Puedes darlo en los ajustes del navegador, o escribir el código.'
          : 'No hay cámara a mano. Escribe el código.',
      );
      return;
    }
    try {
      this.lector = await crearLector();
      video.srcObject = this.flujo;
      await video.play();
      this.estado.set({ tipo: 'mirando' });
      this.programar();
    } catch {
      // La cámara va, pero el lector no ha cargado (sin red la primera vez, navegador viejo).
      this.apagarCamara();
      this.sinCamara('No hemos podido preparar el lector. Escribe el código, que también vale.');
    }
  }

  private sinCamara(motivo: string): void {
    this.estado.set({ tipo: 'sin-camara', motivo });
    this.manual.set(true);
  }

  private programar(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    if (this.estado().tipo !== 'mirando') return;
    this.temporizador = setTimeout(() => void this.mirar(), CADA_MS);
  }

  private async mirar(): Promise<void> {
    const video = this.video()?.nativeElement;
    if (!video || !this.lector || this.estado().tipo !== 'mirando') return;
    try {
      const codigo = (await this.lector.leer(video)).find(eanValido) ?? null;
      if (codigo && codigo === this.ultimaLectura) {
        this.buscar(codigo);
        return;
      }
      this.ultimaLectura = codigo;
    } catch {
      // Un fotograma que no se puede leer (vídeo aún sin datos): al siguiente.
    }
    this.programar();
  }

  private apagarCamara(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    if (this.temporizadorAviso) clearTimeout(this.temporizadorAviso);
    this.flujo?.getTracks().forEach((pista) => pista.stop());
    this.flujo = null;
  }
}
