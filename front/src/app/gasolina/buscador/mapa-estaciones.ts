import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  afterNextRender,
  effect,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import * as L from 'leaflet';

import { Coordenadas, EstacionCercana } from '../core/api.model';
import { CARBURANTES_DESTACADOS, precioDe, urlComoLlegar } from '../core/como-llegar';

/**
 * Mapa con las estaciones encontradas.
 *
 * Se usa Leaflet con teselas de OpenStreetMap en lugar de Google Maps: no hace falta
 * clave de API, ni tarjeta, ni hay cuotas que vigilar. Para pintar unos puntos y
 * centrar el mapa, Google no aporta nada que compense eso.
 *
 * Los marcadores son {@link L.divIcon}, es decir, HTML puro. Además de evitar el
 * problema clásico de que Leaflet no encuentra sus PNG al empaquetar con Angular,
 * permite enseñar el precio dentro del propio marcador, que es justo lo que se viene a
 * mirar.
 *
 * Componente presentacional: recibe datos y avisa de los clics. No pide nada al servidor.
 */
@Component({
  selector: 'app-mapa-estaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="mapa" #contenedor role="application" aria-label="Mapa de gasolineras"></div>`,
  styles: `
    :host {
      display: block;
      block-size: 100%;
    }
    .mapa {
      block-size: 100%;
      inline-size: 100%;
      border-radius: 0.75rem;
      z-index: 0;
    }
  `,
})
export class MapaEstaciones implements OnDestroy {
  readonly estaciones = input.required<readonly EstacionCercana[]>();
  readonly centro = input.required<Coordenadas>();
  readonly seleccionada = input<string | null>(null);
  readonly favoritos = input<ReadonlySet<string>>(new Set());
  /**
   * Viajes: la línea de la ruta, {@code [latitud, longitud]}. Con ella se pinta el camino
   * con su salida y su llegada, se encuadra la ruta entera y no sale «Estás aquí».
   */
  readonly ruta = input<readonly (readonly number[])[] | null>(null);

  readonly estacionElegida = output<EstacionCercana>();

  private readonly contenedor = viewChild.required<ElementRef<HTMLElement>>('contenedor');

  private mapa?: L.Map;
  private capaMarcadores?: L.LayerGroup;

  /** Marcador de cada estación, para poder abrir su globo al elegirla desde la lista. */
  private readonly marcadores = new Map<string, L.Marker>();

  constructor() {
    // El mapa necesita un div ya presente y con tamaño: por eso se crea después del
    // primer render y no en el constructor.
    afterNextRender(() => this.crearMapa());

    // Los efectos leen los inputs, así que se vuelven a ejecutar solos cuando cambian.
    // Si el mapa aún no existe (primer render), no hacen nada: crearMapa() pinta al final.
    effect(() => {
      const estaciones = this.estaciones();
      const seleccionada = this.seleccionada();
      // Se lee aquí dentro para que marcar una favorita repinte su marcador al momento.
      this.favoritos();
      this.ruta();
      if (this.mapa) {
        this.pintarMarcadores(estaciones, seleccionada);
      }
    });

    effect(() => {
      const centro = this.centro();
      this.mapa?.setView([centro.latitud, centro.longitud], this.mapa.getZoom());
    });
  }

  ngOnDestroy(): void {
    // Sin esto, cada vez que se saliera y volviera a entrar en la página quedaría un
    // mapa huérfano escuchando eventos del navegador.
    this.mapa?.remove();
  }

  private crearMapa(): void {
    const centro = this.centro();

    this.mapa = L.map(this.contenedor().nativeElement, {
      center: [centro.latitud, centro.longitud],
      zoom: 14,
      // El scroll del ratón sobre el mapa se queda atrapado y el usuario no puede seguir
      // bajando por la página. Con esto, para hacer zoom hay que pulsar Ctrl.
      scrollWheelZoom: false,

      // SIN ANIMACIÓN DE APARICIÓN, y no es por gusto: EL MAPA SE QUEDABA EN BLANCO.
      //
      // Leaflet muestra cada tesela con un fundido: la crea con opacity 0 y la sube a 1
      // en un requestAnimationFrame. Ese ciclo se cancela cada vez que llegan teselas
      // nuevas, y aquí llegan constantemente porque al repintar los marcadores se llama a
      // fitBounds. Resultado: teselas descargadas y colocadas, pero con opacity 0 para
      // siempre. El mapa existía, medía lo que debía y no se veía nada.
      //
      // Es de los fallos más desconcertantes que hay, porque no da ningún error: ni en la
      // consola, ni en la red, ni en el CSS. Solo se ve inspeccionando el estilo inline
      // de una tesela.
      fadeAnimation: false,
      zoomAnimation: false,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; colaboradores de OpenStreetMap',
    }).addTo(this.mapa);

    this.capaMarcadores = L.layerGroup().addTo(this.mapa);
    this.pintarMarcadores(this.estaciones(), this.seleccionada());
  }

  private pintarMarcadores(estaciones: readonly EstacionCercana[], seleccionada: string | null): void {
    if (!this.mapa || !this.capaMarcadores) {
      return;
    }

    this.capaMarcadores.clearLayers();
    this.marcadores.clear();

    const centro = this.centro();
    const ruta = this.ruta();
    if (ruta && ruta.length > 1) {
      this.pintarRuta(ruta, this.capaMarcadores);
    } else {
      L.circleMarker([centro.latitud, centro.longitud], {
        radius: 7,
        color: '#2563eb',
        fillColor: '#3b82f6',
        fillOpacity: 1,
        weight: 3,
      })
        .bindTooltip('Estás aquí')
        .addTo(this.capaMarcadores);
    }

    for (const estacion of estaciones) {
      const marcador = L.marker([estacion.latitud, estacion.longitud], {
        icon: this.icono(estacion, estacion.id === seleccionada),
        title: `${estacion.rotulo} — ${estacion.direccion}`,
        alt: estacion.rotulo,
      });

      // El globo se construye solo la primera vez que se abre, no al pintar los 50
      // marcadores: la mayoría no se abren nunca.
      marcador.bindPopup(() => globoDeEstacion(estacion), { minWidth: 210 });
      marcador.on('click', () => this.estacionElegida.emit(estacion));
      marcador.addTo(this.capaMarcadores);

      this.marcadores.set(estacion.id, marcador);
    }

    if (ruta && ruta.length > 1) {
      this.mapa.fitBounds(L.latLngBounds(ruta.map((p): L.LatLngExpression => [p[0], p[1]])), {
        padding: [30, 30],
        animate: false,
      });
    } else {
      this.encuadrar(estaciones, centro);
    }

    // Elegir una estación en la lista abre su globo en el mapa: es lo que hace que las
    // dos mitades de la pantalla se sientan como una sola.
    if (seleccionada) {
      this.marcadores.get(seleccionada)?.openPopup();
    }
  }

  /** La carretera en tinta con su salida (círculo) y su llegada (bandera de cuadros). */
  private pintarRuta(ruta: readonly (readonly number[])[], capa: L.LayerGroup): void {
    const linea = ruta.map((p): L.LatLngExpression => [p[0], p[1]]);
    // Debajo, un trazo blanco más ancho: la ruta se lee sobre cualquier color del mapa
    L.polyline(linea, { color: '#ffffff', weight: 9, opacity: 0.9, interactive: false }).addTo(capa);
    L.polyline(linea, { color: '#2a2f5a', weight: 5, opacity: 0.9, interactive: false }).addTo(capa);
    const salida = ruta[0];
    const llegada = ruta[ruta.length - 1];
    L.circleMarker([salida[0], salida[1]], {
      radius: 8,
      color: '#ffffff',
      fillColor: '#2a2f5a',
      fillOpacity: 1,
      weight: 3,
    })
      .bindTooltip('Salida')
      .addTo(capa);
    L.marker([llegada[0], llegada[1]], {
      icon: L.divIcon({ className: 'llegada', html: '🏁', iconSize: [28, 28], iconAnchor: [6, 26] }),
      title: 'Llegada',
      alt: 'Llegada',
      keyboard: false,
    }).addTo(capa);
  }

  /** Ajusta el zoom para que se vean todos los resultados y la posición del usuario. */
  private encuadrar(estaciones: readonly EstacionCercana[], centro: Coordenadas): void {
    if (!this.mapa || estaciones.length === 0) {
      return;
    }

    const puntos: L.LatLngExpression[] = [
      [centro.latitud, centro.longitud],
      ...estaciones.map((e): L.LatLngExpression => [e.latitud, e.longitud]),
    ];

    // animate: false por lo mismo que fadeAnimation arriba: un fitBounds animado en mitad
    // de la carga de teselas deja el fundido a medias y el mapa en blanco.
    this.mapa.fitBounds(L.latLngBounds(puntos), {
      padding: [30, 30],
      maxZoom: 15,
      animate: false,
    });
  }

  private icono(estacion: EstacionCercana, resaltada: boolean): L.DivIcon {
    // El precio del carburante buscado va en el propio marcador; si la búsqueda no
    // filtraba por ninguno, se enseña el rótulo.
    const etiqueta =
      estacion.precio !== null ? `${estacion.precio.toFixed(3)} €` : estacion.rotulo;

    const esFavorita = this.favoritos().has(estacion.id);

    // Elemento DOM en lugar de una cadena de HTML: el rótulo viene de un servicio
    // externo y con textContent no hay nada que interpretar.
    const marcador = document.createElement('span');
    marcador.className = 'marcador';
    if (esFavorita) {
      marcador.classList.add('marcador--favorito');
    }
    if (resaltada) {
      marcador.classList.add('marcador--activo');
    }
    // La estrella va en su propio elemento: así el precio se sigue leyendo como un
    // número y no como "★1,639 €".
    if (esFavorita) {
      const estrella = document.createElement('span');
      estrella.className = 'marcador__estrella';
      estrella.textContent = '★';
      marcador.append(estrella);
    }
    marcador.append(document.createTextNode(etiqueta));

    return L.divIcon({
      className: '',
      html: marcador,
      // Las favoritas necesitan algo más de ancho para que quepa la estrella.
      iconSize: [esFavorita ? 76 : 64, 26],
      iconAnchor: [esFavorita ? 38 : 32, 26],
    });
  }
}

/**
 * Contenido del globo que sale al pulsar un marcador: los dos precios que se miran de
 * verdad y el botón para arrancar la ruta.
 *
 * Se construye con el DOM (createElement + textContent) en vez de con una plantilla de
 * texto. Aquí entran rótulo y dirección de un servicio externo, y con textContent el
 * navegador no interpreta nada de lo que venga: no hay forma de colar etiquetas.
 */
function globoDeEstacion(estacion: EstacionCercana): HTMLElement {
  const globo = document.createElement('div');
  globo.className = 'globo';

  const titulo = document.createElement('p');
  titulo.className = 'globo__titulo';
  titulo.textContent = estacion.rotulo || 'Sin rótulo';
  globo.append(titulo);

  const direccion = document.createElement('p');
  direccion.className = 'globo__direccion';
  direccion.textContent = `${estacion.direccion}, ${estacion.municipio}`;
  globo.append(direccion);

  const precios = document.createElement('ul');
  precios.className = 'globo__precios';

  for (const carburante of CARBURANTES_DESTACADOS) {
    const precio = precioDe(estacion, carburante.clave);

    const fila = document.createElement('li');
    const nombre = document.createElement('span');
    nombre.textContent = carburante.etiqueta;

    const valor = document.createElement('strong');
    // Una gasolinera puede no vender diésel, y entonces no hay precio que enseñar.
    // Poner un 0 o dejarlo en blanco confundiría; el guion dice "aquí no hay".
    valor.textContent = precio === null ? '—' : `${precio.toFixed(3)} €/l`;
    if (precio === null) {
      valor.className = 'globo__sin-precio';
    }

    fila.append(nombre, valor);
    precios.append(fila);
  }
  globo.append(precios);

  if (estacion.horario) {
    const horario = document.createElement('p');
    horario.className = 'globo__horario';
    horario.textContent = estacion.horario;
    globo.append(horario);
  }

  const comoLlegar = document.createElement('a');
  comoLlegar.className = 'globo__boton';
  comoLlegar.href = urlComoLlegar(estacion);
  comoLlegar.target = '_blank';
  // noopener evita que la pestaña de Google Maps pueda tocar la nuestra desde window.opener.
  comoLlegar.rel = 'noopener noreferrer';
  comoLlegar.textContent = 'Cómo llegar';
  globo.append(comoLlegar);

  return globo;
}
