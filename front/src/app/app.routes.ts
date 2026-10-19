import { Routes } from '@angular/router';

import { conSesion, sinSesion } from './core/sesion';
import { guiaLaPrimeraVez } from './ayuda/guia';
import { conHogar } from './hogar/con-hogar.guard';
import { enlaceDeInvitacion } from './hogar/invitacion';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [sinSesion],
    loadComponent: () => import('./bienvenida/bienvenida.page').then((m) => m.BienvenidaPage),
    title: 'Agarrao · Que no se te escape ni un céntimo',
  },
  {
    path: 'lista',
    canActivate: [conSesion, conHogar, guiaLaPrimeraVez],
    loadComponent: () => import('./lista/lista.page').then((m) => m.ListaPage),
    title: 'Agarrao · La lista',
  },
  {
    path: 'lista/donde',
    canActivate: [conSesion, conHogar],
    loadComponent: () => import('./reparto/reparto.page').then((m) => m.RepartoPage),
    title: 'Agarrao · ¿Dónde lo compro?',
  },
  {
    path: 'hogar',
    canActivate: [conSesion, conHogar, guiaLaPrimeraVez],
    loadComponent: () => import('./hogar/hogar.page').then((m) => m.HogarPage),
    title: 'Agarrao · Tu hogar',
  },
  {
    path: 'precios',
    canActivate: [conSesion, conHogar, guiaLaPrimeraVez],
    loadComponent: () => import('./precios/precios.page').then((m) => m.PreciosPage),
    title: 'Agarrao · Precios',
  },
  {
    path: 'gasto',
    canActivate: [conSesion, conHogar, guiaLaPrimeraVez],
    loadComponent: () => import('./gasto/gasto.page').then((m) => m.GastoPage),
    title: 'Agarrao · Gasto',
  },
  {
    path: 'anadir',
    canActivate: [conSesion, conHogar, guiaLaPrimeraVez],
    loadComponent: () => import('./anadir/anadir.page').then((m) => m.AnadirPage),
    title: 'Agarrao · Añadir',
  },
  {
    path: 'gastos/nuevo',
    canActivate: [conSesion, conHogar],
    loadComponent: () => import('./gasto/anotar/anotar-gasto.page').then((m) => m.AnotarGastoPage),
    title: 'Agarrao · Anotar un gasto',
  },
  {
    path: 'gastos/:id',
    canActivate: [conSesion, conHogar],
    loadComponent: () => import('./gasto/anotar/anotar-gasto.page').then((m) => m.AnotarGastoPage),
    title: 'Agarrao · Cambiar el gasto',
  },
  {
    path: 'guia',
    canActivate: [conSesion],
    loadComponent: () => import('./ayuda/guia.page').then((m) => m.GuiaPage),
    title: 'Agarrao · Cómo funciona',
  },
  {
    path: 'novedades',
    canActivate: [conSesion],
    loadComponent: () => import('./ayuda/novedades.page').then((m) => m.NovedadesPage),
    title: 'Agarrao · Novedades',
  },
  {
    // Gasolina (lo que era Gasofa): buscar es público; lo demás pide sesión.
    path: 'gasolina',
    loadComponent: () => import('./gasolina/gasolina.page').then((m) => m.GasolinaPage),
    children: [
      {
        path: '',
        loadComponent: () => import('./gasolina/buscador/buscador.page').then((m) => m.BuscadorPage),
        title: 'Agarrao · Gasolineras cerca de ti',
      },
      {
        // Público, como el buscador: la ruta y las gasolineras de camino no piden cuenta
        path: 'viajes',
        loadComponent: () => import('./gasolina/viajes/viajes.page').then((m) => m.ViajesPage),
        title: 'Agarrao · Viajes',
      },
      {
        path: 'favoritas',
        canActivate: [conSesion],
        loadComponent: () => import('./gasolina/favoritos/favoritos.page').then((m) => m.FavoritosPage),
        title: 'Agarrao · Gasolineras favoritas',
      },
      {
        path: 'repostajes',
        canActivate: [conSesion, conHogar],
        loadComponent: () => import('./gasolina/repostajes/repostajes.page').then((m) => m.RepostajesPage),
        title: 'Agarrao · Repostajes',
      },
      {
        path: 'vehiculos',
        canActivate: [conSesion, conHogar],
        loadComponent: () => import('./gasolina/repostajes/vehiculos.page').then((m) => m.VehiculosPage),
        title: 'Agarrao · Coches',
      },
    ],
  },
  {
    path: 'escanear',
    canActivate: [conSesion, conHogar],
    loadComponent: () => import('./escaner/escaner.page').then((m) => m.EscanerPage),
    title: 'Agarrao · Escanear',
  },
  {
    path: 'tickets',
    canActivate: [conSesion, conHogar],
    children: [
      {
        path: '',
        loadComponent: () => import('./tickets/tickets.page').then((m) => m.TicketsPage),
        title: 'Agarrao · Tickets',
      },
      {
        path: 'nuevo',
        loadComponent: () => import('./tickets/nuevo-ticket.page').then((m) => m.NuevoTicketPage),
        title: 'Agarrao · Subir ticket',
      },
      {
        path: ':id',
        loadComponent: () => import('./tickets/ticket.page').then((m) => m.TicketPage),
        title: 'Agarrao · Ticket',
      },
    ],
  },
  {
    path: 'empezar',
    canActivate: [conSesion],
    loadComponent: () => import('./empezar/empezar.page').then((m) => m.EmpezarPage),
    title: 'Agarrao · Empezar',
  },
  // El enlace que se comparte al invitar. La guarda decide adónde ir; nunca se pinta.
  { path: 'unirse/:codigo', canActivate: [enlaceDeInvitacion], children: [] },
  // La pantalla de la primera versión: quien la tenga guardada cae en la lista.
  // Públicas: las enlaza la pantalla de consentimiento de Google (proyecto «agarrao»)
  {
    path: 'privacidad',
    loadComponent: () => import('./legal/privacidad.page').then((m) => m.PrivacidadPage),
    title: 'Agarrao · Privacidad',
  },
  {
    path: 'condiciones',
    loadComponent: () => import('./legal/condiciones.page').then((m) => m.CondicionesPage),
    title: 'Agarrao · Condiciones de uso',
  },
  { path: 'inicio', redirectTo: 'lista' },
  { path: '**', redirectTo: '' },
];
