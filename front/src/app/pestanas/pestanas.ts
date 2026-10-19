import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavController } from '@ionic/angular';

import { Parte, PartesService } from '../core/partes.service';

export type Pestana = 'lista' | 'precios' | 'anadir' | 'gasto' | 'gasolina' | 'hogar';

interface Destino {
  id: Pestana;
  nombre: string;
  ruta: string;
  /** Trazos del icono (viewBox 0 0 24 24). */
  icono: string;
  /** La parte que la enciende; sin parte, siempre está. */
  parte?: Parte;
}

const ANADIR: Destino = { id: 'anadir', nombre: 'Añadir', ruta: '/anadir', icono: 'M12 5v14M5 12h14' };

const DESTINOS: readonly Destino[] = [
  {
    id: 'lista',
    nombre: 'Lista',
    ruta: '/lista',
    icono: 'M9 6h11M9 12h11M9 18h11M4 6l.01 0M4 12l.01 0M4 18l.01 0',
    parte: 'LISTA',
  },
  {
    id: 'precios',
    nombre: 'Precios',
    ruta: '/precios',
    icono: 'M3 12V4.5A1.5 1.5 0 0 1 4.5 3H12l9 9-9 9zM7.5 7.5l.01 0',
    parte: 'PRECIOS',
  },
  { id: 'gasto', nombre: 'Gasto', ruta: '/gasto', icono: 'M4 20V11M10 20V5M16 20v-6M21 20H3', parte: 'GASTOS' },
  {
    id: 'gasolina',
    nombre: 'Gasolina',
    ruta: '/gasolina',
    icono: 'M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 21h12M7 9h4M14 11h2a2 2 0 0 1 2 2v3a1.5 1.5 0 0 0 3 0V8l-3-3',
    parte: 'GASOLINA',
  },
  { id: 'hogar', nombre: 'Hogar', ruta: '/hogar', icono: 'M4 11l8-7 8 7v9H4zM10 20v-5h4v5' },
];

/**
 * La barra de abajo: Lista · Precios · ＋ · Gasto · Gasolina · Hogar, sin las partes que la persona
 * haya apagado en Hogar. El ＋ va siempre en medio: subir un ticket, anotar
 * un gasto o apuntar algo en la lista. Va en el pie de cada pantalla principal (debajo de
 * lo que esa pantalla tenga fijo, como el campo de apuntar).
 *
 * Cambiar de pestaña no es avanzar: se navega a la raíz y sin animación, como en una app.
 */
@Component({
  selector: 'app-pestanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <nav class="pestanas" aria-label="Secciones" [style.grid-template-columns]="'repeat(' + destinos().length + ', 1fr)'">
      @for (d of destinos(); track d.id) {
        <a
          class="pestana"
          [class.pestana--mas]="d.id === 'anadir'"
          [class.pestana--aqui]="d.id === activa()"
          [routerLink]="d.ruta"
          [attr.aria-current]="d.id === activa() ? 'page' : null"
          (click)="ir($event, d)"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true"><path [attr.d]="d.icono" /></svg>
          <span class="pestana__nombre" [class.sr-solo]="d.id === 'anadir'">{{ d.nombre }}</span>
        </a>
      }
    </nav>
  `,
  styles: `
    :host {
      display: block;
      padding: 0 16px max(10px, env(safe-area-inset-bottom));
    }

    .pestanas {
      display: grid;
      gap: 4px;
      max-width: 528px;
      margin-inline: auto;
      padding: 5px;
      border-radius: 999px;
      background: var(--papel);
      box-shadow: var(--sombra-ficha);
    }

    .pestana {
      display: grid;
      justify-items: center;
      align-content: center;
      gap: 1px;
      min-height: 52px;
      border-radius: 999px;
      color: var(--tinta-suave);
      text-decoration: none;
      transition: background 260ms var(--salida), color 200ms ease;
      -webkit-tap-highlight-color: transparent;
    }

    .pestana svg {
      width: 24px;
      height: 24px;
      fill: none;
      stroke: currentColor;
      stroke-width: 2.2;
      stroke-linecap: round;
      stroke-linejoin: round;
      transition: transform 380ms var(--salida);
    }

    .pestana__nombre {
      font-size: 0.75rem;
      font-weight: 800;
      line-height: 1.2;
    }

    /* Donde estás: placa lila, tinta plena y el icono un pelín más arriba */
    .pestana--aqui {
      background: var(--placa-nombre-lila);
      color: var(--tinta);
    }

    .pestana--aqui svg {
      transform: translateY(-1px) scale(1.08);
    }

    @media (hover: hover) {
      .pestana:not(.pestana--aqui):hover {
        background: var(--lila);
        color: var(--tinta);
      }
    }

    .pestana:active svg {
      transform: scale(0.9);
    }

    /* El ＋: botón rosa redondo en medio, el gesto principal de la app */
    .pestana--mas {
      justify-self: center;
      align-self: center;
      width: 52px;
      background: var(--rosa-boton);
      color: #fff;
      box-shadow: 0 6px 14px -6px rgb(212 35 97 / 0.7);
    }

    .pestana--mas svg {
      width: 26px;
      height: 26px;
      stroke-width: 2.8;
    }

    .pestana--mas.pestana--aqui,
    .pestana--mas:not(.pestana--aqui):hover {
      background: var(--rosa-boton-hondo);
      color: #fff;
    }
  `,
})
export class Pestanas {
  private readonly nav = inject(NavController);
  private readonly partes = inject(PartesService);

  /** La pantalla en la que está la barra. */
  readonly activa = input.required<Pestana>();

  /** Las encendidas, con el ＋ después de las dos primeras (en medio si están todas). */
  protected readonly destinos = computed(() => {
    const encendidas = this.partes.partes();
    const visibles = DESTINOS.filter((d) => !d.parte || encendidas.includes(d.parte));
    const antes = visibles.filter((d) => d.id === 'lista' || d.id === 'precios');
    const despues = visibles.filter((d) => d.id !== 'lista' && d.id !== 'precios');
    return [...antes, ANADIR, ...despues];
  });

  constructor() {
    this.partes.cargar();
  }

  protected ir(evento: MouseEvent, destino: Destino): void {
    // Con Ctrl o el botón central, que el navegador haga lo suyo (abrir en otra pestaña).
    if (evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.button !== 0) return;
    evento.preventDefault();
    if (destino.id === this.activa()) return;
    void this.nav.navigateRoot(destino.ruta, { animated: false });
  }
}
