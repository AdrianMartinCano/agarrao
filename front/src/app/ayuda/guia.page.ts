import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonContent, NavController } from '@ionic/angular';

import { Parte, PartesService } from '../core/partes.service';
import { GuiaService } from './guia';

interface Paso {
  titulo: string;
  texto: string[];
  personaje: string;
  /** La parte de la que habla; sin parte, vale para todos. */
  parte?: Parte;
  color: string;
}

const PASOS: readonly Paso[] = [
  {
    titulo: 'El botón ＋',
    personaje: 'listillo',
    color: 'var(--rubor)',
    texto: [
      'Está en medio de la barra de abajo. Desde ahí se mete todo: subir un ticket, anotar un gasto o apuntar algo en la lista.',
      'Lo que metas se cuenta solo en las gráficas de Gasto.',
    ],
  },
  {
    titulo: 'La lista',
    personaje: 'huevo',
    color: 'var(--menta)',
    parte: 'LISTA',
    texto: [
      'Es de toda la casa: lo que apunta uno lo ven los demás al momento.',
      'Escribe lo que falta o escanea el código de barras del envase. Al comprar, tócalo para echarlo al carro.',
      '«¿Dónde lo compro?» reparte la lista por el súper donde cada cosa sale más barata.',
    ],
  },
  {
    titulo: 'Los tickets',
    personaje: 'leche',
    color: 'var(--cielo)',
    texto: [
      'Sube el PDF de la app del súper (Mercadona, Lidl, Dia…) o hazle una foto al de papel.',
      'Repasa las líneas y guarda. Lo que comprasteis se tacha solo de la lista y sus precios van a Precios.',
      'Si el ticket no es de un súper (un restaurante, la gasolinera), se guarda como gasto.',
    ],
  },
  {
    titulo: 'Precios',
    personaje: 'tomate',
    color: 'var(--mantequilla)',
    parte: 'PRECIOS',
    texto: [
      'Cada producto, por kilo o por litro, en cada súper: así se ve qué sale más barato de verdad.',
      'Busca cualquier cosa y toca un producto para ver cuánto vale ese mismo en cada súper.',
      'Los precios son los de la web de cada súper o de vuestros tickets; en la tienda pueden variar algo.',
    ],
  },
  {
    titulo: 'Gastos',
    personaje: 'chocolate',
    color: 'var(--lila)',
    parte: 'GASTOS',
    texto: [
      'La hucha se llena con lo del mes: el súper, restaurantes, gasolina, suscripciones…',
      'Los tarritos de abajo son los últimos meses: tócalos para verlos.',
      'Lo que se repite cada mes (Netflix, Spotify) se anota una vez marcando «Se repite cada mes».',
    ],
  },
  {
    titulo: 'Gasolina',
    personaje: 'aceite',
    color: 'var(--cielo)',
    parte: 'GASOLINA',
    texto: [
      'Las gasolineras más baratas cerca de ti, en lista y en el mapa, con los precios del Ministerio de cada media hora.',
      'Da de alta vuestros coches y apunta cada repostaje: verás cuánto consume de verdad y cuánto ahorras frente a la zona.',
      'Cada repostaje cuenta una vez en Gasto, como gasolina. Buscar gasolineras se puede hasta sin cuenta.',
    ],
  },
  {
    titulo: 'Hogar',
    personaje: 'hogar',
    color: 'var(--rubor)',
    texto: [
      'Invita a los tuyos con el código o el enlace.',
      'En «Qué usas» apagas lo que no quieras ver. Solo cambia para ti.',
      'Esta guía y las novedades de cada versión están siempre ahí.',
    ],
  },
];

/** La guía «Cómo funciona»: sale sola la primera vez y se abre siempre desde Hogar. */
@Component({
  selector: 'app-guia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IonContent, RouterLink],
  template: `
    <ion-content class="trama">
      <main class="guia">
        <header class="guia__cabeza">
          <img class="guia__listillo" src="personajes/listillo-cartel.svg" alt="" width="120" height="120" />
          <h1 class="titular">Cómo funciona</h1>
          <p class="guia__lema">Que no se te escape ni un céntimo. En un minuto:</p>
        </header>

        <ol class="pasos">
          @for (p of pasos(); track p.titulo; let i = $index) {
            <li class="paso" [style.background]="p.color" [style.--giro]="i % 2 === 0 ? '-1deg' : '0.8deg'">
              <img class="paso__personaje" [src]="'personajes/' + p.personaje + '.svg'" alt="" width="56" height="56" />
              <div class="paso__cuerpo">
                <h2 class="paso__titulo">{{ p.titulo }}</h2>
                @for (t of p.texto; track t) {
                  <p class="paso__texto">{{ t }}</p>
                }
              </div>
            </li>
          }
        </ol>

        <button type="button" class="boton-rosa" (click)="entendido()">¡Entendido!</button>
        <a class="guia__novedades" routerLink="/novedades">Ver las novedades de cada versión</a>
      </main>
    </ion-content>
  `,
  styles: `
    .guia {
      display: grid;
      align-content: start;
      gap: 22px;
      max-width: 528px;
      margin-inline: auto;
      padding: max(22px, env(safe-area-inset-top)) 16px max(28px, env(safe-area-inset-bottom));
    }

    .guia__cabeza {
      display: grid;
      justify-items: center;
      gap: 6px;
      text-align: center;
    }

    .guia__listillo {
      width: 120px;
      height: 120px;
      animation: asoma 700ms var(--salida) both;
    }

    .guia__lema {
      margin: 0;
      max-width: 30ch;
      color: var(--tinta-suave);
      text-wrap: balance;
    }

    .pasos {
      display: grid;
      gap: 18px;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    /* Cada paso, una ficha de color inclinada a mano con su personaje asomando */
    .paso {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 16px;
      border: 3px solid var(--papel);
      border-radius: var(--radio-ficha);
      box-shadow: var(--sombra-ficha);
      transform: rotate(var(--giro));
    }

    .paso__personaje {
      flex: none;
      width: 56px;
      height: 56px;
    }

    .paso__cuerpo {
      display: grid;
      gap: 6px;
      min-width: 0;
    }

    .paso__titulo {
      margin: 0;
      font-family: var(--letra-burbuja);
      font-weight: 800;
      font-size: 1.375rem;
      line-height: 1.1;
    }

    .paso__texto {
      margin: 0;
      font-size: 0.9375rem;
      text-wrap: pretty;
    }

    .guia__novedades {
      justify-self: center;
      min-height: 44px;
      padding: 10px 6px;
      color: var(--tinta);
      font-weight: 800;
      text-decoration: underline;
      text-decoration-color: var(--rosa-boton);
      text-decoration-thickness: 2px;
      text-underline-offset: 3px;
    }

    @keyframes asoma {
      from {
        opacity: 0;
        transform: translateY(14px) scale(0.92);
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .guia__listillo { animation: none; }
    }
  `,
})
export class GuiaPage {
  private readonly nav = inject(NavController);
  private readonly guia = inject(GuiaService);
  private readonly partes = inject(PartesService);

  /** Solo lo de las partes que usa la persona. */
  protected readonly pasos = computed(() =>
    PASOS.filter((p) => !p.parte || this.partes.partes().includes(p.parte)),
  );

  constructor() {
    this.guia.marcarVista();
  }

  /** A la primera parte que use: la lista, o Gasto, o Precios. */
  protected entendido(): void {
    const partes = this.partes.partes();
    const ruta = partes.includes('LISTA') ? '/lista' : partes.includes('GASTOS') ? '/gasto' : '/precios';
    void this.nav.navigateRoot(ruta);
  }
}
