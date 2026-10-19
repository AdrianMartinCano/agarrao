import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, firstValueFrom, isObservable, of, throwError } from 'rxjs';

import { conHogar, PRIMER_HOGAR } from './con-hogar.guard';
import { HogaresService } from './hogares.service';

describe('conHogar', () => {
  const creados: string[] = [];
  let hogares: unknown[] | null;
  let crearFalla: boolean;

  async function pasar(): Promise<boolean | UrlTree> {
    const r = TestBed.runInInjectionContext(() => conHogar({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    return isObservable(r) ? firstValueFrom(r as Observable<boolean | UrlTree>) : (r as boolean | UrlTree);
  }

  beforeEach(() => {
    creados.length = 0;
    crearFalla = false;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: HogaresService,
          useValue: {
            hogares: () => null,
            cargar: () => of(hogares),
            crear: (nombre: string) => {
              creados.push(nombre);
              return crearFalla ? throwError(() => new Error('sin red')) : of({ id: 'h1' });
            },
          },
        },
      ],
    });
  });

  it('sin hogar, se crea «Mi casa» y se entra sin pasar por Empezar', async () => {
    hogares = [];
    expect(await pasar()).toBe(true);
    expect(creados).toEqual([PRIMER_HOGAR]);
  });

  it('con hogar, se entra sin crear nada', async () => {
    hogares = [{ id: 'h1' }];
    expect(await pasar()).toBe(true);
    expect(creados).toEqual([]);
  });

  it('si no se puede crear, a Empezar como antes', async () => {
    hogares = [];
    crearFalla = true;
    const r = await pasar();
    expect(r instanceof UrlTree && r.toString()).toBe('/empezar');
  });
});
