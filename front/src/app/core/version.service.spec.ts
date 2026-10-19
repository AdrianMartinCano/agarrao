import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SwUpdate } from '@angular/service-worker';

import { VERSION_APP } from './version-app';
import { VersionService } from './version.service';

describe('VersionService', () => {
  let servicio: VersionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SwUpdate, useValue: { isEnabled: false } },
      ],
    });
    servicio = TestBed.inject(VersionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pregunta con la versión de esta build y guarda lo que decide el back', () => {
    servicio.comprobar().subscribe();

    const peticion = http.expectOne((r) => r.url === '/api/app/version');
    expect(peticion.request.params.get('actual')).toBe(VERSION_APP);
    peticion.flush({
      data: { estado: 'OBLIGATORIA', actual: VERSION_APP, ultima: '9.0.0', minima: '9.0.0', notas: null },
    });

    expect(servicio.comprobacion()?.estado).toBe('OBLIGATORIA');
  });

  it('descartar solo cierra el aviso opcional, nunca el obligatorio', () => {
    servicio.comprobar().subscribe();
    http.expectOne((r) => r.url === '/api/app/version').flush({
      data: { estado: 'OBLIGATORIA', actual: VERSION_APP, ultima: '9.0.0', minima: '9.0.0', notas: null },
    });

    servicio.descartar();

    expect(servicio.comprobacion()?.estado).toBe('OBLIGATORIA');
  });
});
