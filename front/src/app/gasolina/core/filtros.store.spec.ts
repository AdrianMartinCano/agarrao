import { TestBed } from '@angular/core/testing';

import { FiltrosStore } from './filtros.store';

const CLAVE = 'gaspath.filtros';

/**
 * Lo que se guarda en localStorage es texto editable por el usuario y puede venir de una
 * versión anterior de la aplicación. Estos tests cubren que nada de eso llegue al backend
 * en forma de filtro inválido.
 */
describe('FiltrosStore', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  function crear(): FiltrosStore {
    return TestBed.inject(FiltrosStore);
  }

  it('arranca con la gasolina 95 y 5 km si no hay nada guardado', () => {
    const store = crear();

    expect(store.filtros()).toEqual({
      carburante: 'GASOLINA_95_E5',
      radioKm: 5,
      orden: 'DISTANCIA',
    });
  });

  it('recuerda los filtros entre visitas', () => {
    localStorage.setItem(
      CLAVE,
      JSON.stringify({ carburante: 'GASOLEO_A', radioKm: 25, orden: 'PRECIO' }),
    );

    expect(crear().filtros()).toEqual({
      carburante: 'GASOLEO_A',
      radioKm: 25,
      orden: 'PRECIO',
    });
  });

  it('guarda el cambio en cuanto se hace', () => {
    const store = crear();

    store.actualizar((actuales) => ({ ...actuales, radioKm: 10 }));

    expect(JSON.parse(localStorage.getItem(CLAVE) ?? '{}').radioKm).toBe(10);
  });

  it('descarta un radio que ya no existe en la aplicación', () => {
    // Por ejemplo, guardado por una versión anterior que ofrecía 7 km.
    localStorage.setItem(CLAVE, JSON.stringify({ carburante: null, radioKm: 7, orden: 'DISTANCIA' }));

    expect(crear().filtros().radioKm).toBe(5);
  });

  it('no deja "ordenar por precio" sin carburante', () => {
    // El backend responde 400 a esa combinación. La interfaz no deja formarla, pero sí
    // puede quedar guardada de una versión vieja o editada a mano.
    localStorage.setItem(CLAVE, JSON.stringify({ carburante: null, radioKm: 5, orden: 'PRECIO' }));

    expect(crear().filtros().orden).toBe('DISTANCIA');
  });

  it('sobrevive a un localStorage corrupto', () => {
    localStorage.setItem(CLAVE, 'esto no es json {{{');

    expect(crear().filtros().radioKm).toBe(5);
    // Y se limpia, para no repetir el error en cada arranque.
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });
});
