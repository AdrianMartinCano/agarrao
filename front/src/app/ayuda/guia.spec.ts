import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';

import { GuiaService, guiaLaPrimeraVez } from './guia';

describe('guía de la primera vez', () => {
  function pasar(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      guiaLaPrimeraVez({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('la primera vez manda a la guía', () => {
    const resultado = pasar();
    expect(resultado instanceof UrlTree && resultado.toString()).toBe('/guia');
  });

  it('una vez vista deja pasar, también en la próxima visita', () => {
    TestBed.inject(GuiaService).marcarVista();
    expect(pasar()).toBe(true);
    expect(localStorage.getItem('guia-vista')).toBe('1');
  });
});
