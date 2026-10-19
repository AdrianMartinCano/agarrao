import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { HogaresService } from '../hogar/hogares.service';
import { GastoPage } from './gasto.page';
import { Gasto, GastoService } from './gasto.service';

type Pagina = GastoPage & Record<string, any>; // eslint-disable-line @typescript-eslint/no-explicit-any

function gasto(cambio: Partial<Gasto>): Gasto {
  return {
    mes: '2026-10',
    total: 0,
    tickets: 0,
    anotados: 0,
    porTipo: [],
    descuentos: 0,
    referencia: 100,
    meses: [],
    porTienda: [],
    porCategoria: [],
    top: [],
    ...cambio,
  };
}

describe('GastoPage', () => {
  let resumen: Gasto;

  function crear(): Pagina {
    const fixture = TestBed.overrideComponent(GastoPage, { set: { template: '' } }).createComponent(GastoPage);
    fixture.detectChanges();
    return fixture.componentInstance as Pagina;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: HogaresService, useValue: { activo: () => ({ id: 'h1' }) } },
        {
          provide: GastoService,
          useValue: { resumen: () => of(resumen), anotados: () => of([]), fijos: () => of([]) },
        },
      ],
    });
  });

  it('solo súper: la hucha sigue por pasillos', () => {
    resumen = gasto({
      total: 50,
      tickets: 2,
      porTipo: [{ tipo: 'SUPER', total: 50, veces: 2 }],
      porCategoria: [
        { categoria: 'LACTEOS', total: 30 },
        { categoria: 'FRUTA', total: 20 },
      ],
    });
    const pagina = crear();
    expect(pagina['porTipo']()).toBe(false);
    expect(pagina['capas']().map((c: { nombre: string }) => c.nombre)).toEqual(['Lácteos y huevos', 'Fruta']);
    expect(pagina['cuantos'](resumen)).toBe('en 2 tickets');
  });

  it('con más cosas que súper, la hucha va por tipo', () => {
    resumen = gasto({
      total: 120,
      tickets: 1,
      anotados: 2,
      porTipo: [
        { tipo: 'RESTAURANTE', total: 70, veces: 1 },
        { tipo: 'SUPER', total: 50, veces: 1 },
      ],
      porCategoria: [{ categoria: 'FRUTA', total: 50 }],
    });
    const pagina = crear();
    expect(pagina['porTipo']()).toBe(true);
    expect(pagina['capas']().map((c: { nombre: string }) => c.nombre)).toEqual(['Restaurantes', 'Súper']);
    expect(pagina['goloso']()).toBe('Restaurantes se come más de la mitad de la hucha.');
    expect(pagina['cuantos'](resumen)).toBe('en un ticket y 2 gastos anotados');
  });

  it('con un solo tipo no hay nota: es todo', () => {
    resumen = gasto({ total: 46.8, anotados: 1, porTipo: [{ tipo: 'RESTAURANTE', total: 46.8, veces: 1 }] });
    const pagina = crear();
    expect(pagina['capas']()).toHaveLength(1);
    expect(pagina['goloso']()).toBeNull();
  });
});
