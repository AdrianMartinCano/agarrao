import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  afterNextRender,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';


// Chart.js 4 no registra nada por su cuenta: sin esto, cualquier gráfica falla con un
// "is not a registered controller".
//
// Se registra pieza a pieza en lugar de usar `registerables`, que arrastra TODOS los
// tipos —radar, polar, burbujas, tarta, dispersión— y sus escalas. Aquí solo hay líneas
// y barras: declarar lo que se usa deja fuera del paquete unos 60 kB que nadie descarga
// para nada.
Chart.register(
  LineController,
  BarController,
  LineElement,
  PointElement,
  BarElement,
  CategoryScale,
  LinearScale,
  Filler, // el relleno bajo la línea del consumo
  Tooltip,
  Legend,
);

/**
 * Una gráfica de Chart.js.
 *
 * Chart.js dibuja sobre un canvas, así que sus ejes, rejilla y leyenda son píxeles, no
 * CSS: se repinta al cambiar la configuración. (En Gasofa también al cambiar de tema;
 * Agarrao no tiene tema oscuro.)
 *
 * Los colores se leen de las variables CSS del tema en vez de estar escritos aquí: así
 * las gráficas y el resto de la página no pueden acabar diciendo cosas distintas.
 */
@Component({
  selector: 'app-grafica',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<canvas #lienzo [attr.aria-label]="descripcion()" role="img"></canvas>`,
  styles: `
    :host {
      display: block;
      position: relative;
      block-size: 100%;
    }
  `,
})
export class Grafica implements OnDestroy {
  /** Configuración sin colores: los pone este componente según el tema. */
  readonly configuracion = input.required<ChartConfiguration>();

  /** Para quien no ve la gráfica: qué muestra y cuál es la conclusión. */
  readonly descripcion = input.required<string>();

  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  /** Los colores salen de las variables de `.gasolina` (src/gasolina.scss), que viven en el armazón, no en :root. */
  private readonly anfitrion = inject<ElementRef<HTMLElement>>(ElementRef);

  private grafica?: Chart;

  constructor() {
    // El canvas necesita existir y tener tamaño: por eso después del primer render.
    afterNextRender(() => this.pintar());

    effect(() => {
      this.configuracion();
      if (this.grafica) {
        this.pintar();
      }
    });
  }

  ngOnDestroy(): void {
    // Chart.js se engancha a eventos del canvas y del window; sin destruirla, cada visita
    // a la pestaña deja una gráfica huérfana escuchando.
    this.grafica?.destroy();
  }

  private pintar(): void {
    this.grafica?.destroy();

    const estilo = getComputedStyle(this.anfitrion.nativeElement);
    const color = (nombre: string) => estilo.getPropertyValue(nombre).trim();

    const tinta = color('--tinta-suave');
    const borde = color('--borde');

    const configuracion = structuredClone(this.configuracion()) as ChartConfiguration;

    this.grafica = new Chart(this.lienzo().nativeElement, {
      ...configuracion,
      data: this.colorear(configuracion, color),
      options: {
        ...configuracion.options,
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          ...configuracion.options?.plugins,
          legend: {
            display: (configuracion.data.datasets?.length ?? 0) > 1,
            labels: { color: tinta, boxWidth: 12, boxHeight: 12, usePointStyle: true },
          },
          tooltip: {
            backgroundColor: color('--tinta'),
            titleColor: color('--superficie'),
            bodyColor: color('--superficie'),
            padding: 10,
            displayColors: false,
          },
        },
        scales: {
          x: {
            ticks: { color: tinta, font: { size: 11 } },
            grid: { display: false },
            border: { color: borde },
          },
          y: {
            ticks: { color: tinta, font: { size: 11 } },
            grid: { color: borde },
            border: { display: false },
            ...configuracion.options?.scales?.['y'],
          },
        },
      },
    });
  }

  /** Aplica la paleta de la aplicación a los datos, respetando lo que ya venga puesto. */
  private colorear(configuracion: ChartConfiguration, color: (n: string) => string) {
    const paleta = [color('--ambar'), color('--azul'), color('--verde')];

    return {
      ...configuracion.data,
      datasets: configuracion.data.datasets.map((conjunto, i) => ({
        borderColor: paleta[i % paleta.length],
        backgroundColor:
          configuracion.type === 'bar'
            ? paleta[i % paleta.length]
            : `color-mix(in srgb, ${paleta[i % paleta.length]} 15%, transparent)`,
        borderWidth: 2,
        pointBackgroundColor: paleta[i % paleta.length],
        pointRadius: 3,
        tension: 0.25,
        ...conjunto,
      })),
    };
  }
}
