import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@adrianmartincano/ng-auth';

import { PartesService } from './partes.service';

describe('PartesService', () => {
  let http: HttpTestingController;

  function crear(): PartesService {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: () => ({ id: 'u1' }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(PartesService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('sin nada guardado, todas encendidas (nadie pierde nada al estrenarlo)', () => {
    const partes = crear();
    expect(partes.partes()).toEqual(['LISTA', 'PRECIOS', 'GASTOS', 'GASOLINA']);
  });

  it('pregunta una sola vez por persona y ordena lo que llega', () => {
    const partes = crear();
    partes.cargar();
    partes.cargar();
    http.expectOne('/api/yo/partes').flush({ data: ['GASTOS', 'LISTA'] });

    expect(partes.partes()).toEqual(['LISTA', 'GASTOS']);
    expect(partes.precios()).toBe(false);
    expect(localStorage.getItem('partes')).toBe('LISTA,GASTOS');
  });

  it('arranca con la última copia guardada e ignora partes que ya no existen', () => {
    localStorage.setItem('partes', 'BICICLETA,PRECIOS');
    const partes = crear();
    expect(partes.partes()).toEqual(['PRECIOS']);
  });

  it('elegir guarda en el back y cambia la barra con lo que contesta', () => {
    const partes = crear();
    partes.elegir(['PRECIOS']).subscribe();

    const peticion = http.expectOne('/api/yo/partes');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ partes: ['PRECIOS'] });
    peticion.flush({ data: ['PRECIOS'] });
    expect(partes.lista()).toBe(false);
    expect(partes.precios()).toBe(true);
  });
});
