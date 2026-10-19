# Listillo · front

PWA con Angular 21 (standalone, signals, **zoneless**) + Ionic 9 (solo estructura, modo
`md`) + `@angular/service-worker`.

## Comandos

```bash
npm install     # necesita GITHUB_TOKEN en el entorno (lo lee .npmrc) para @adrianmartincano/ng-*
npm start       # http://localhost:4200, proxy de /api a localhost:8080 (proxy.conf.json)
npm run build   # dist/front/browser, con service worker
npm test        # vitest
```

En `npm start` (configuración `development`) la portada enseña **«Entrar con el usuario de
prueba»**: `angular.json` sustituye `core/acceso-prueba.ts` (null) por
`acceso-prueba.development.ts` (el usuario que crea `ListilloLocal` en el back). En
producción la clave no llega al paquete y el botón no se pinta.

> **npm 11.5.1 falla** al instalar con `Cannot read properties of null (reading 'edgesOut')`.
> Usa npm ≥ 11.21 (`npx -y npm@11.21.0 install`). El Dockerfile ya lo hace.

## Estructura

```
src/app/
  core/          versión (servicio + version-app.ts generado)
  version/       aviso opcional (cinta) y pantalla de versión obligatoria
  personajes/    la ficha de personaje del catálogo
  core/          + api.ts (sobre ApiResponse, mensajes de error) y sesion.ts (guardas
                 propias e interceptor de sesión caducada)
  bienvenida/    entrar con Google
  empezar/       primer uso / otro hogar: crear o unirse con código
  hogar/         pantalla del hogar (invitar, miembros, salir), servicio de hogares,
                 guarda conHogar (sin hogar, crea «Mi casa» sola: no se pasa por Empezar), enlace /unirse/:codigo, colores de cinta por persona
  lista/         la lista compartida: servicio optimista + filas con cinta
  escaner/       la lupa: cámara, lector (nativo o zxing-wasm), ficha del producto
  tickets/       subir (PDF o foto), repasar en la tira, listado y detalle; ocr.ts;
                 clasificar-linea (genérico y tamaño de una línea, también en Precios)
  precios/       catálogo de genéricos por pasillo y el cara a cara de cada uno
  gasto/         el mes como tarro de hucha (hucha.ts; por tipo o por pasillo), meses,
                 anotados, fijos, tiendas y lo que más cuesta; anotar/ (anotar o cambiar
                 un gasto, rellenable desde un ticket)
  anadir/        el ＋: subir ticket, anotar gasto o apuntar en la lista
  ayuda/         guía «Cómo funciona» (guarda guiaLaPrimeraVez, localStorage «guia-vista»)
                 y Novedades (de core/novedades.ts, generado desde ../CHANGELOG.md)
  core/          + partes.service.ts (qué partes usa la persona; copia en localStorage)
  gasolina/      lo que era Gasofa: gasolina.page (armazón con Buscar · Viajes · Favoritas ·
                 Repostajes · Coches; públicos Buscar y Viajes), viajes/ (ruta y gasolineras
                 de camino; el mapa del buscador pinta la ruta con su input `ruta`), buscador/ (Leaflet), favoritos/,
                 repostajes/ (Chart.js) y core/; sus colores salen de src/gasolina.scss
  pestanas/      la barra de abajo (Lista · Precios · ＋ · Gasto · Gasolina · Hogar, sin lo apagado)
  reparto/       ¿Dónde lo compro?: la lista repartida por súper, un resguardo por tienda
public/
  personajes/    los personajes SVG (dibujados a mano; los de pasillo, con tools/personajes.py)
  fuentes/       Sniglet y M PLUS Rounded 1c, servidas desde aquí (offline)
  iconos/        iconos PWA, renderizados con tools/iconos.html
```

## Notas técnicas

- **La versión solo se escribe en `package.json`.** `tools/escribir-version.mjs` la copia
  a `src/app/core/version-app.ts` antes de `start`, `build` y `test`. No se importa
  `package.json` desde la app para no meter el fichero entero en el bundle.
- **Si `/api/app/version` falla, no se bloquea nada**: la app tiene que abrir en el súper
  sin cobertura.
- **«Actualizar» = el service worker busca, activa y se recarga.** En una PWA instalada no
  hay botón de recargar.
- **Login con el botón de `ng-auth`** (el widget de Google va tapado por el nuestro). Si el
  back tiene Google apagado (`/api/auth/google/config` da 404), la pantalla lo dice.
- **Guardas propias** (`core/sesion.ts`), no las de `ng-auth`: las de la librería mandan a
  `/login` (aquí no existe) y la de invitados redirige a «/», que es la propia portada.
- **Lista optimista**: los cambios se ven al momento; cada petición lleva número y solo
  cuenta la respuesta de la última, para que una vieja no pise una nueva. Mientras hay un
  cambio en camino no se refresca. Si un cambio falla, se avisa y se vuelve a pedir la lista.
- **Lo de los demás llega cada 10 s** (y al volver a la pantalla), solo con la pestaña
  visible. Sin WebSocket ni SSE: con JWT en cabecera, `EventSource` no puede mandarlo, y
  para una lista de la compra 10 s sobran.
- **Invitar** comparte `https://<dominio>/unirse/<código>` con `navigator.share` (o lo copia).
  Sin sesión, el código se guarda en `localStorage` y tras entrar se lleva a «Empezar» con
  él escrito: nunca se une a nadie sin que lo confirme.
- **Color de cinta por persona**: por orden de llegada al hogar (`hogar/cintas.ts`), el
  mismo en la lista y en la pantalla del hogar. Seis colores (los cuatro de los personajes
  más lechuga y uva): hasta la sexta persona no se repite ninguno.
- **Antes de unirse se ve a qué hogar**: con el código completo (8 caracteres) se pide
  `/api/hogares/invitacion/{codigo}` y la tarjeta enseña nombre y personas, o el error. Con
  RxJS (`toObservable` + `switchMap`) y no con `httpResource`, que sumaba 10 kB al paquete
  inicial y lo sacaba del presupuesto de 500 kB.
- **Los puntos suspensivos no salen de M PLUS Rounded** (`fuentes.css`): es japonesa y los
  dibuja centrados.
- **Escáner**: `BarcodeDetector` del navegador si lee EAN (Chrome Android); si no,
  `barcode-detector` 3.2.2 (ponyfill de zxing-wasm), cargado solo en `/escanear`. Su
  `zxing_reader.wasm` (1 MB) se copia de `node_modules/zxing-wasm/dist/reader` a `/zxing/`
  (angular.json) y el service worker lo guarda al primer uso (grupo `lector`, lazy): sin
  CDN de terceros y funciona sin red una vez visto. Un código se acepta cuando se lee igual
  dos veces seguidas y cuadra su dígito de control. Sin cámara o sin permiso, se escribe a
  mano. Para capturas: Chrome sin cabeza con `--use-fake-device-for-media-stream`.
- **Tickets con foto, gratis y sin sacar la foto del móvil**: `tesseract.js` 7 (español,
  LSTM, modelo `4.0.0_best_int` de `@tesseract.js-data/spa`). Worker, motor (`*-lstm.wasm.js`,
  uno de tres según el navegador, ~4 MB) y modelo (2 MB) se sirven desde `/tesseract/`
  (angular.json) y el service worker los guarda al primer uso. Antes de leer, la foto se
  pasa a grises y se estira el contraste (`ocr.ts`). El PDF se sube tal cual y lo lee el
  back. `allowedCommonJsDependencies`: tesseract.js no se publica como ESM.
- **Presupuesto inicial a 560 kB** (aviso): Ionic + Angular ya son ~390 kB del chunk
  principal; lo que se descarga comprimido son ~139 kB.
- **Fuentes servidas desde `public/fuentes`**, no desde Google Fonts, para que se vean
  sin conexión.
- **Iconos PNG**: no hay ImageMagick. Se sacan con Chrome sin cabeza desde
  `tools/iconos.html` (instrucciones dentro). Ojo: Chrome sin cabeza no baja de 500 px de
  ancho; para capturas de móvil, meter la app en un iframe de 390 px.
- **Pestañas en el pie de cada pantalla principal**, no con `ion-tabs`: cada página pone
  `<app-pestanas activa="…">` en su `ion-footer` (debajo de lo que tenga fijo, como el campo
  de apuntar). Cambiar de pestaña navega a la raíz **sin animación** (`navigateRoot`).
- **Retratos de genérico**: el propio del genérico (`retrato`, p. ej. `leche.svg`) o el del
  pasillo (`generico.model.ts`). Los de pasillo salen de `tools/personajes.py` y los 30 de
  los genéricos más comprados (plátano, pollo asado, yogur, aceite…) de
  `tools/personajes_genericos.py`, que reutiliza su `cara()` y `svg()`:
  `python tools/personajes.py public/personajes` y
  `python tools/personajes_genericos.py public/personajes`. Qué genérico lleva cuál lo dice
  la migración V21 del back. Ojo con el blanco: el retrato va sobre papel blanco, así que un
  cuerpo blanco no se ve (el yogur es lila por eso).
- **Los gráficos son SVG y CSS propios**, sin librería de gráficos: el tarro, los tarritos
  y las barras de tienda no necesitan más.
- **Estadísticas de visitas con Umami** (1.2.4): el Umami de Pimon (`stats.pimon3d.com`), sin
  cookies, en `src/index.html`. Reutiliza la web que fue de Gasofa, renombrada a Agarrao
  (mismo Website ID). `data-domains` para no contar `localhost`, `data-exclude-search` para no
  mandar la query y `data-before-send` para que `/unirse/<código>` llegue como `/unirse` (el
  código de invitación no debe acabar en Umami). No hay CSP que tocar; si algún día la hay,
  `stats.pimon3d.com` va en `script-src` y en `connect-src`. Cualquier cambio aquí cambia
  también `app/legal/privacidad.page.ts`.
- **`favicon.ico` además del SVG** (2026-10-13): sin él, `/favicon.ico` daba 404 y el
  servicio de iconos de DuckDuckGo (el que usa el panel de Umami) y otros que no leen SVG no
  sacaban el icono de Agarrao. Es la cara de `favicon.svg` rasterizada (16, 32, 48 y 64 px);
  si cambia el SVG, regenerarlo.
