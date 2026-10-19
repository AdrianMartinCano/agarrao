import { EstacionCercana } from './api.model';
import { precioDe, urlComoLlegar, urlViajeConParada } from './como-llegar';

describe('comoLlegar', () => {
  const estacion: EstacionCercana = {
    id: '4982',
    rotulo: 'CEPSA',
    direccion: 'CL PASEO ACACIAS, 8',
    municipio: 'Madrid',
    provincia: 'MADRID',
    codigoPostal: '28005',
    horario: 'L-D: 24H',
    latitud: 40.4021,
    longitud: -3.7048,
    distanciaKm: 1.38,
    precio: 1.82,
    precios: { GASOLINA_95_E5: 1.82, GASOLEO_A: 1.75 },
  };

  it('lleva a Google Maps con las coordenadas, no con el texto de la dirección', () => {
    const url = urlComoLlegar(estacion);

    // Las direcciones del Ministerio vienen abreviadas ("CL PASEO ACACIAS") y Google las
    // interpreta mal; la coordenada no admite interpretación.
    expect(url).toContain('destination=40.4021%2C-3.7048');
    expect(url).not.toContain('ACACIAS');
  });

  it('usa el formato oficial de enlaces de Google Maps', () => {
    expect(urlComoLlegar(estacion)).toMatch(/^https:\/\/www\.google\.com\/maps\/dir\/\?api=1/);
  });

  it('devuelve el precio de un carburante que se vende', () => {
    expect(precioDe(estacion, 'GASOLEO_A')).toBe(1.75);
  });

  it('devuelve null si la estación no vende ese carburante', () => {
    // Distinguirlo de un cero importa: un 0 saldría como la gasolinera más barata.
    expect(precioDe(estacion, 'GLP')).toBeNull();
  });

  it('en Viajes, el viaje entero con la gasolinera de parada', () => {
    const url = urlViajeConParada({ latitud: 40.4, longitud: -3.7 }, { latitud: 39.47, longitud: -0.37 }, estacion);

    expect(url).toContain('origin=40.4%2C-3.7');
    expect(url).toContain('destination=39.47%2C-0.37');
    expect(url).toContain('waypoints=40.4021%2C-3.7048');
  });
});
