import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Lugar, Viaje } from '../core/api.model';
import { EstacionesService } from '../core/estaciones.service';
import { GeolocalizacionService } from '../core/geolocalizacion.service';
import { ViajesPage } from './viajes.page';

const MADRID: Lugar = { nombre: 'Madrid', detalle: 'Madrid', tipo: 'MUNICIPIO', latitud: 40.4, longitud: -3.7, estaciones: 900 };
const VALENCIA: Lugar = { nombre: 'Valencia', detalle: 'Valencia', tipo: 'MUNICIPIO', latitud: 39.47, longitud: -0.37, estaciones: 300 };

const VIAJE: Viaje = {
  distanciaKm: 357,
  duracionMin: 241,
  desvioKm: 2,
  ruta: [
    [40.4, -3.7],
    [39.47, -0.37],
  ],
  enRuta: 2,
  mediaRuta: 1.6,
  minimaRuta: 1.5,
  estaciones: [],
};

describe('ViajesPage', () => {
  let pedidas: unknown[][];
  let falla: boolean;

  /** Acceso a lo protegido sin pelearse con TypeScript. */
  type Pagina = Record<string, any>;

  function crear(): Pagina {
    pedidas = [];
    TestBed.configureTestingModule({
      providers: [
        {
          provide: EstacionesService,
          useValue: {
            carburantes: () => of([{ clave: 'GASOLINA_95_E5', etiqueta: 'Gasolina 95' }]),
            lugares: () => of([]),
            ruta: (...args: unknown[]) => {
              pedidas.push(args);
              return falla
                ? throwError(() => ({ error: { message: 'No hay ruta en coche entre esos dos sitios' } }))
                : of(VIAJE);
            },
          },
        },
        { provide: GeolocalizacionService, useValue: { posicionActual: () => of({ latitud: 40.0, longitud: -3.0 }) } },
      ],
    });
    TestBed.overrideComponent(ViajesPage, { set: { template: '', imports: [] } });
    return TestBed.createComponent(ViajesPage).componentInstance as unknown as Pagina;
  }

  beforeEach(() => (falla = false));

  it('no busca hasta tener salida y llegada', () => {
    const pagina = crear();
    expect(pagina['listo']()).toBe(false);

    pagina['elegirOrigen'](MADRID);
    expect(pagina['listo']()).toBe(false);

    pagina['elegirDestino'](VALENCIA);
    expect(pagina['listo']()).toBe(true);
  });

  it('pide la ruta con el carburante y el desvío elegidos', () => {
    const pagina = crear();
    pagina['elegirOrigen'](MADRID);
    pagina['elegirDestino'](VALENCIA);
    pagina['desvioKm'].set(5);

    pagina['buscar']();

    expect(pedidas).toHaveLength(1);
    expect(pedidas[0][2]).toBe('GASOLINA_95_E5');
    expect(pedidas[0][3]).toBe(5);
    expect(pagina['viaje']()).toEqual(VIAJE);
  });

  it('si no hay ruta, cuenta el porqué que manda el back', () => {
    falla = true;
    const pagina = crear();
    pagina['elegirOrigen'](MADRID);
    pagina['elegirDestino'](VALENCIA);

    pagina['buscar']();

    expect(pagina['error']()).toBe('No hay ruta en coche entre esos dos sitios');
    expect(pagina['viaje']()).toBeNull();
  });

  it('la duración se lee en horas y minutos', () => {
    const pagina = crear();
    expect(pagina['duracion'](241)).toBe('4 h 1 min');
    expect(pagina['duracion'](45)).toBe('45 min');
  });
});
