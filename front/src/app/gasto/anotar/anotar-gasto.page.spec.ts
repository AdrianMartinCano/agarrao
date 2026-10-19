import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NavController } from '@ionic/angular';

import { HogaresService } from '../../hogar/hogares.service';
import { AnotarGastoPage, nombreDeTicket } from './anotar-gasto.page';

const URL = '/api/hogares/h1/gastos';

/** Acceso a lo protegido de la página, solo para el test. */
type Pagina = AnotarGastoPage & Record<string, any>; // eslint-disable-line @typescript-eslint/no-explicit-any

describe('AnotarGastoPage', () => {
  let http: HttpTestingController;
  let navegado: { ruta: string; mes?: string } | null;

  function crear(id?: string): Pagina {
    const fixture = TestBed.createComponent(AnotarGastoPage);
    if (id) fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    return fixture.componentInstance as Pagina;
  }

  function enviar(pagina: Pagina): void {
    pagina['guardar'](new Event('submit'));
  }

  beforeEach(() => {
    navegado = null;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: HogaresService, useValue: { activo: () => ({ id: 'h1' }) } },
        {
          provide: NavController,
          useValue: {
            navigateRoot: (ruta: string, extras?: { queryParams?: { mes?: string } }) => {
              navegado = { ruta, mes: extras?.queryParams?.mes };
              return Promise.resolve(true);
            },
          },
        },
      ],
    });
    TestBed.overrideComponent(AnotarGastoPage, { set: { template: '' } });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('anota con coma decimal y vuelve a Gasto en el mes del gasto', () => {
    const pagina = crear();
    pagina['concepto'].set('  Cena con los primos ');
    pagina['importe'].set('46,8');
    pagina['fecha'].set('2026-09-20');
    pagina['tipo'].set('RESTAURANTE');
    pagina['cadaMes'].set(true);
    enviar(pagina);

    const peticion = http.expectOne(URL);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      concepto: 'Cena con los primos',
      importe: 46.8,
      fecha: '2026-09-20',
      tipo: 'RESTAURANTE',
      cadaMes: true,
    });
    peticion.flush({ data: {} });
    expect(navegado).toEqual({ ruta: '/gasto', mes: '2026-09' });
  });

  it('no envía nada sin tipo ni con un importe que no es un número', () => {
    const pagina = crear();
    pagina['concepto'].set('Gasolina');
    pagina['importe'].set('mucho');
    enviar(pagina);
    expect(pagina['error']()).toContain('cuánto');

    pagina['importe'].set('60');
    enviar(pagina);
    expect(pagina['error']()).toContain('tipo');
    http.expectNone(URL);
  });

  it('al cambiar uno, lo carga, lo guarda con PUT y nunca crea un fijo', () => {
    const pagina = crear('g1');
    http.expectOne(`${URL}/g1`).flush({
      data: { id: 'g1', concepto: 'Netflix', importe: 13.99, fecha: '2026-10-05', tipo: 'SUSCRIPCION', fijoId: 'f1' },
    });
    expect(pagina['importe']()).toBe('13,99');

    pagina['cadaMes'].set(true);
    pagina['importe'].set('15,99');
    enviar(pagina);
    const peticion = http.expectOne(`${URL}/g1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.cadaMes).toBe(false);
    expect(peticion.request.body.importe).toBe(15.99);
    peticion.flush({ data: {} });
  });

  it('borrar pide un segundo toque', () => {
    const pagina = crear('g1');
    http.expectOne(`${URL}/g1`).flush({
      data: { id: 'g1', concepto: 'Cine', importe: 18, fecha: '2026-10-01', tipo: 'OCIO', fijoId: null },
    });
    pagina['borrar']();
    http.expectNone(`${URL}/g1`);
    pagina['borrar']();
    expect(http.expectOne(`${URL}/g1`).request.method).toBe('DELETE');
  });

  it('el nombre del ticket deja de gritar', () => {
    expect(nombreDeTicket('RESTAURANTE LA TAGLIATELLA')).toBe('Restaurante La Tagliatella');
    expect(nombreDeTicket('Bar Manolo')).toBe('Bar Manolo');
    expect(nombreDeTicket('ESTACIÓN DE SERVICIO ÁLAMO')).toBe('Estación De Servicio Álamo');
  });
});
