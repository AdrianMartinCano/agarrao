import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@adrianmartincano/ng-auth';
import { IonContent, IonFooter } from '@ionic/angular';
import { Observable } from 'rxjs';

import { mensajeDeError } from '../core/api';
import { Parte, PARTES, PartesService } from '../core/partes.service';
import { PerfilService } from '../core/perfil';
import { VERSION_APP } from '../core/version-app';
import { Cadena, NOMBRE_CADENA } from '../tickets/ticket.model';
import { Pestanas } from '../pestanas/pestanas';
import { colorDeCinta, inicial } from './cintas';
import { Hogar, Miembro } from './hogar.model';
import { HogaresService } from './hogares.service';

/**
 * El hogar abierto: quién está, invitar (enlace + código) y las salidas. Lo que se puede
 * deshacer mal (salir, sacar a alguien) pide un segundo toque en el mismo sitio en vez de
 * un diálogo del navegador.
 */
@Component({
  selector: 'app-hogar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, IonFooter, RouterLink, Pestanas],
  templateUrl: './hogar.page.html',
  styleUrl: './hogar.page.scss',
})
export class HogarPage {
  private readonly hogares = inject(HogaresService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly perfil = inject(PerfilService);
  protected readonly partes = inject(PartesService);

  protected readonly todasLasPartes = PARTES;
  protected readonly version = VERSION_APP;
  protected readonly parte: Record<Parte, { nombre: string; nota: string }> = {
    LISTA: { nombre: 'Lista', nota: 'La lista de la compra de la casa.' },
    PRECIOS: { nombre: 'Precios', nota: 'Dónde es más barato cada cosa.' },
    GASTOS: { nombre: 'Gastos', nota: 'Restaurantes, suscripciones… y las gráficas.' },
    GASOLINA: { nombre: 'Gasolina', nota: 'Gasolineras baratas cerca, tus coches y sus repostajes.' },
  };

  /** Las cadenas que se pueden elegir; las de la primera fila tienen precios de su web. */
  protected readonly cadenasConWeb: readonly Cadena[] = [
    'MERCADONA', 'CONSUM', 'AHORRAMAS', 'EROSKI', 'CAPRABO', 'ALDI', 'LIDL',
  ];
  protected readonly cadenasSoloTickets: readonly Cadena[] = [
    'CARREFOUR', 'DIA', 'ALCAMPO', 'EL_CORTE_INGLES', 'FAMILY_CASH', 'COSTCO', 'GADIS', 'BONPREU',
  ];
  protected readonly nombreCadena = NOMBRE_CADENA;
  protected readonly editandoMiNombre = signal(false);
  protected readonly miNombre = computed(() => this.auth.currentUser()?.name ?? '');

  protected readonly hogar = signal<Hogar | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  /** «Copiado» o «Enlace compartido» un par de segundos. */
  protected readonly hecho = signal<string | null>(null);
  /** Qué acción espera su segundo toque: 'salir' o el id del miembro a sacar. */
  protected readonly confirmando = signal<string | null>(null);
  protected readonly editandoNombre = signal(false);

  protected readonly otrosHogares = computed(() =>
    (this.hogares.hogares() ?? []).filter((h) => h.id !== this.hogar()?.id),
  );
  protected readonly soyPropietario = computed(() => this.hogar()?.miRol === 'PROPIETARIO');
  protected readonly miId = computed(() => this.auth.currentUser()?.id ?? null);
  /** «ABCD 2345»: en dos bloques se dicta mejor por teléfono. */
  protected readonly codigoLegible = computed(() => {
    const c = this.hogar()?.codigoInvitacion ?? '';
    return `${c.slice(0, 4)} ${c.slice(4)}`;
  });

  constructor() {
    this.cargar();
  }

  protected readonly cinta = colorDeCinta;
  protected readonly inicial = inicial;

  protected async invitar(): Promise<void> {
    const h = this.hogar();
    if (!h) return;
    const url = `${location.origin}/unirse/${h.codigoInvitacion}`;
    const texto = `Apúntate a la lista de la compra de «${h.nombre}» en Agarrao. Código: ${this.codigoLegible()}`;
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Agarrao', text: texto, url });
        this.avisar('¡Invitación enviada!');
      } else {
        await navigator.clipboard.writeText(`${texto}\n${url}`);
        this.avisar('Enlace copiado. Pégalo donde quieras.');
      }
    } catch (e) {
      // Cerrar el menú de compartir no es un error.
      if (!(e instanceof DOMException && e.name === 'AbortError')) {
        this.error.set('No se ha podido compartir. Dicta el código, que también vale.');
      }
    }
  }

  protected nuevoCodigo(): void {
    const h = this.hogar();
    if (!h) return;
    this.lanzar(this.hogares.nuevoCodigo(h.id), (nuevo) => {
      this.hogar.set(nuevo);
      this.avisar('Código nuevo. El anterior ya no vale.');
    });
  }

  protected guardarNombre(evento: Event, campo: HTMLInputElement): void {
    evento.preventDefault();
    const h = this.hogar();
    const nombre = campo.value.trim();
    if (!h || !nombre || nombre === h.nombre) {
      this.editandoNombre.set(false);
      return;
    }
    this.lanzar(this.hogares.renombrar(h.id, nombre), (nuevo) => {
      this.hogar.set(nuevo);
      this.editandoNombre.set(false);
    });
  }

  protected guardarMiNombre(evento: Event, campo: HTMLInputElement): void {
    evento.preventDefault();
    const nombre = campo.value.trim();
    if (!nombre || nombre === this.miNombre()) {
      this.editandoMiNombre.set(false);
      return;
    }
    this.lanzar(this.perfil.cambiarNombre(nombre), () => {
      this.editandoMiNombre.set(false);
      this.avisar('Nombre cambiado.');
      this.cargar();
    });
  }

  protected compara(c: Cadena): boolean {
    return this.hogar()?.cadenas.includes(c) ?? false;
  }

  /** Marca o desmarca un súper; al menos uno tiene que quedar. */
  /** Encender o apagar una parte de la app (solo para ti). Al menos una se queda. */
  protected alternarParte(p: Parte): void {
    const ahora = this.partes.partes();
    const elegidas = ahora.includes(p) ? ahora.filter((x) => x !== p) : [...ahora, p];
    if (elegidas.length === 0) {
      this.error.set('Deja al menos una parte encendida.');
      return;
    }
    this.error.set(null);
    this.partes.elegir(elegidas).subscribe({
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha guardado. Revisa la conexión.')),
    });
  }

  protected alternarCadena(c: Cadena): void {
    const h = this.hogar();
    if (!h) return;
    const elegidas = h.cadenas.includes(c) ? h.cadenas.filter((x) => x !== c) : [...h.cadenas, c];
    if (elegidas.length === 0) {
      this.error.set('Deja al menos un supermercado.');
      return;
    }
    this.error.set(null);
    this.hogar.set({ ...h, cadenas: elegidas });
    this.lanzar(this.hogares.elegirCadenas(h.id, elegidas), (nuevo) => this.hogar.set(nuevo));
  }

  protected sacar(miembro: Miembro): void {
    const h = this.hogar();
    if (!h) return;
    if (this.confirmando() !== miembro.usuarioId) {
      this.confirmando.set(miembro.usuarioId);
      return;
    }
    this.lanzar(this.hogares.echar(h.id, miembro.usuarioId), (nuevo) => this.hogar.set(nuevo));
  }

  protected salir(): void {
    const h = this.hogar();
    if (!h) return;
    if (this.confirmando() !== 'salir') {
      this.confirmando.set('salir');
      return;
    }
    this.lanzar(this.hogares.salir(h.id), (quedan) =>
      void this.router.navigateByUrl(quedan.length > 0 ? '/lista' : '/empezar', { replaceUrl: true }),
    );
  }

  protected cambiarA(id: string): void {
    this.hogares.elegir(id);
    void this.router.navigateByUrl('/lista');
  }

  protected cerrarSesion(): void {
    this.auth.logout();
    this.hogares.olvidar();
    void this.router.navigateByUrl('/', { replaceUrl: true });
  }

  private cargar(): void {
    const activo = this.hogares.activo();
    if (!activo) {
      void this.router.navigateByUrl('/empezar', { replaceUrl: true });
      return;
    }
    this.hogares.detalle(activo.id).subscribe({
      next: (h) => this.hogar.set(h),
      error: (e: unknown) => this.error.set(mensajeDeError(e, 'No se ha podido cargar el hogar.')),
    });
  }

  private lanzar<T>(peticion: Observable<T>, alAcabar: (valor: T) => void): void {
    this.ocupado.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (valor) => {
        this.ocupado.set(false);
        this.confirmando.set(null);
        alAcabar(valor);
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.confirmando.set(null);
        this.error.set(mensajeDeError(e));
      },
    });
  }

  private avisar(texto: string): void {
    this.hecho.set(texto);
    setTimeout(() => this.hecho.set(null), 2600);
  }
}
