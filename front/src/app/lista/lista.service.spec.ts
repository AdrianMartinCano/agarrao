import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@adrianmartincano/ng-auth';

import { Articulo } from './articulo.model';
import { ListaService } from './lista.service';

const URL = '/api/hogares/h1/lista';

function articulo(cambio: Partial<Articulo> & { id: string; nombre: string }): Articulo {
  return {
    cantidad: 1,
    comprado: false,
    apuntadoPor: 'Ana',
    apuntadoPorId: 'u1',
    ean: null,
    creadoEn: '2026-10-09T10:00:00',
    compradoEn: null,
    ...cambio,
  };
}

describe('ListaService', () => {
  let lista: ListaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: () => ({ id: 'u1' }) } },
      ],
    });
    lista = TestBed.inject(ListaService);
    http = TestBed.inject(HttpTestingController);

    lista.abrir('h1');
    http.expectOne(URL).flush({ data: [articulo({ id: 'a1', nombre: 'Leche' })] });
  });

  afterEach(() => http.verify());

  it('tachar se ve al momento, antes de que conteste el back', () => {
    lista.marcar(lista.pendientes()[0], true);

    expect(lista.pendientes()).toHaveLength(0);
    expect(lista.enElCarro().map((a) => a.nombre)).toEqual(['Leche']);
    http.expectOne(`${URL}/a1`).flush({ data: [articulo({ id: 'a1', nombre: 'Leche', comprado: true })] });
  });

  it('apuntar lo mismo que ya falta suma en vez de duplicar', () => {
    lista.apuntar('  leche ');

    expect(lista.pendientes()).toHaveLength(1);
    expect(lista.pendientes()[0].cantidad).toBe(2);
    http.expectOne(URL).flush({ data: [articulo({ id: 'a1', nombre: 'Leche', cantidad: 2 })] });
  });

  it('una respuesta vieja no pisa un cambio más nuevo', () => {
    lista.marcar(lista.pendientes()[0], true);
    lista.marcar(lista.enElCarro()[0], false);
    const [primera, segunda] = http.match(`${URL}/a1`);

    segunda.flush({ data: [articulo({ id: 'a1', nombre: 'Leche', comprado: false })] });
    primera.flush({ data: [articulo({ id: 'a1', nombre: 'Leche', comprado: true })] });

    expect(lista.pendientes().map((a) => a.nombre)).toEqual(['Leche']);
  });

  it('no pregunta por los demás mientras hay un cambio propio en camino', () => {
    lista.marcar(lista.pendientes()[0], true);
    lista.refrescar();

    // Solo la petición del cambio: el refresco se salta.
    http.expectOne(`${URL}/a1`).flush({ data: [] });
    http.expectNone((r) => r.method === 'GET');
  });

  it('si un cambio falla, lo dice y vuelve a pedir la lista de verdad', () => {
    lista.quitar(lista.pendientes()[0]);
    expect(lista.articulos()).toEqual([]);

    http.expectOne(`${URL}/a1`).flush(null, { status: 0, statusText: 'Sin red' });

    expect(lista.error()).toContain('Sin conexión');
    http.expectOne(URL).flush({ data: [articulo({ id: 'a1', nombre: 'Leche' })] });
    expect(lista.pendientes()).toHaveLength(1);
  });
});
