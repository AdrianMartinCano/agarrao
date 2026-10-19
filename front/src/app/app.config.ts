import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { AuthService, authInterceptor } from '@adrianmartincano/ng-auth';
import { provideIonicAngular } from '@ionic/angular';

import { routes } from './app.routes';
import { sesionCaducada } from './core/sesion';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, sesionCaducada])),
    // Material en todas partes: el aspecto lo pone Listillo, Ionic solo la estructura.
    provideIonicAngular({ mode: 'md' }),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
    // Las rutas del back son las de por defecto de ng-auth (/api/auth/google…). Se dejan
    // dichas aquí para que no haya que ir a mirar la librería.
    provideAppInitializer(() =>
      inject(AuthService).configure({
        googleEndpoint: '/api/auth/google',
        googleConfigEndpoint: '/api/auth/google/config',
      }),
    ),
  ],
};
