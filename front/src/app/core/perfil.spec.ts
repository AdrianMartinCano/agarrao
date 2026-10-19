import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@adrianmartincano/ng-auth';

import { PerfilService } from './perfil';

describe('PerfilService.recuperarNombre', () => {
  let http: HttpTestingController;
  const usuario = signal<{ id: string; email: string; name: string } | null>(null);

  function crear(): PerfilService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { currentUser: usuario } }],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(PerfilService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('recién entrado (el email hace de nombre), trae el nombre guardado', () => {
    usuario.set({ id: 'u1', email: 'yo@example.com', name: 'yo@example.com' });
    crear().recuperarNombre();
    http.expectOne('/api/users/me').flush({ data: { name: 'Adrián' } });

    expect(usuario()?.name).toBe('Adrián');
    expect(JSON.parse(localStorage.getItem('auth_token_user')!).name).toBe('Adrián');
  });

  it('con el nombre ya bueno, no pregunta', () => {
    usuario.set({ id: 'u1', email: 'yo@example.com', name: 'Adrián' });
    crear().recuperarNombre();
    http.expectNone('/api/users/me');
  });
});
