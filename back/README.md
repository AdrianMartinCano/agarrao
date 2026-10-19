# Listillo · back

Spring Boot 3.4.1, Java 21, PostgreSQL 16, Flyway. Paquete `dev.pimon.listillo`.

Libs propias (GitHub Packages, versión lockstep **0.12.5**): `spring-common`,
`spring-security`, `spring-users`, `spring-google`. Hace falta `~/.m2/settings.xml` con el
server `github-adrianmartincano` (ver `librerias-backend/README.md`).

## Comandos

```bash
mvn test              # incluye ContextoArrancaTest: Postgres embebido real + Flyway + validate
mvn spring-boot:run   # necesita Postgres en localhost:5432 (listillo/listillo/listillo)

# Sin Postgres instalado: Postgres embebido en :54320 (datos en .local-db/, fuera de git)
# y usuario de prueba prueba@example.com / Listillo1234! si la base está vacía
mvn spring-boot:test-run -Dspring-boot.run.main-class=dev.pimon.listillo.ListilloLocal
```

## Esquema

Manda **Flyway** (`db/migration`); Hibernate solo valida (`ddl-auto: validate`).

- `V1__tablas_de_librerias.sql`: tablas de las libs (`app_settings`, `app_users`,
  `user_roles`, `tokens_recuperacion`, `cuentas_google`). Las libs traen entidades pero no
  migraciones. **Al subir de versión las libs, columna nueva = migración nueva**:
  `ContextoArrancaTest` lo canta.
- `V2__version_app.sql`: la versión publicada de la app, una fila (`id = 1`).
- `V9__precios_de_tiendas.sql`: `productos_tienda` (cadena + id de la web; nombre,
  formato, precio, tamaño, unidad, precio por unidad, sección, genérico y si se corrigió a
  mano). `V10__pistas_que_excluyen.sql`: pistas con «-» (huevos de codorniz, carcasa de
  pollo, papel húmedo…) y el desodorante en €/L. `V11__mas_pistas_que_excluyen.sql`: las
  vistas con Lidl (patatas de bolsa, café en grano o soluble, galletas como pan tostado).
  `V12__patatas_de_bolsa.sql` (patatas lisas/onduladas como de bolsa) y
  `V13__aceite_y_papel.sql` (aceite extra suave no es virgen extra; papel húmedo «pure»).
  `V14__hogar_y_buscador.sql`: pasillo HOGAR con 15 genéricos de hogar y bazar (y 4 más de
  limpieza) y `productos_tienda.nombre_normalizado` (índice `varchar_pattern_ops`) para buscar.
- `V8__genericos.sql`: `genericos` (catálogo de Listillo con `hogar_id` null + los propios
  de cada hogar; nombre, categoría, unidad UD/KG/L, pistas para adivinar, retrato) con
  sus 95 filas del catálogo, y `clasificaciones` (hogar + cadena + texto normalizado →
  genérico o null = «ninguno», tamaño corregido, origen AUTO/USUARIO).
- `V6__tickets.sql` / `V7__alias_por_hogar.sql`: `tickets`, `lineas_ticket` (texto,
  cantidad, unidad UD/KG/L, precio unitario, importe, con qué se casó) y `alias_ticket`
  (lo aprendido: hogar + cadena + texto normalizado → producto).
- `V5__cantidad_sin_signo_estimado.sql`: limpia «400 g e» en lo ya guardado.
- `V4__productos.sql`: `productos` (PK `ean`, nombre, marca, cantidad en texto, imagen,
  categorías, `origen` OPEN_FOOD_FACTS/USUARIO/DESCONOCIDO, `consultado_en`) y la columna
  `ean` en `articulos_lista`.
- `V22__gastos_y_partes.sql`: `gastos` (lo anotado que no es ticket del súper: concepto,
  importe > 0, fecha, tipo, `fijo_id` con ON DELETE SET NULL; índice único parcial
  `(fijo_id, fecha)` para que un mes de un fijo no se apunte dos veces), `gastos_fijos`
  (concepto, importe, tipo, día 1–28, `ultimo_mes` ya apuntado) y `partes_usuario`
  (usuario → «LISTA,PRECIOS,GASTOS»; sin fila = todas). `V23__version_1_0_0.sql`: anuncia
  la 1.0.0 en `version_app`.
- `V24__gasolina.sql`: `favoritos` (por persona), `vehiculos` y `repostajes` (por
  **hogar**; mismas columnas que Gasofa para pasar sus datos tal cual, más `creado_por`).
  `V25__partes_apagadas.sql`: `partes_usuario` guarda las **apagadas** (las nuevas salen
  encendidas). `V26__version_1_1_0.sql`: anuncia la 1.1.0.
- `V3__hogares_y_lista.sql`: `hogares` (código de invitación único de 8), `miembros_hogar`
  (PK hogar+usuario, rol `PROPIETARIO`/`MIEMBRO`, FK a `app_users` con cascada) y
  `articulos_lista` (cantidad 1–99, quién lo apuntó y quién lo compró, sin FK para que
  sobreviva al borrado de la cuenta).

## API

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/app/version?actual=x.y.z` | público |
| GET | `/api/admin/app/version` | ROLE_ADMIN |
| PUT | `/api/admin/app/version` `{ultima, minima, notas}` | ROLE_ADMIN |
| GET | `/api/auth/google/config` | público (spring-google; 404 si está apagado) |
| POST | `/api/auth/google` `{credential}` | público (spring-google) |
| POST | `/api/auth/register` | **cerrado (404)** salvo con `listillo.registro-con-contrasena=true` (solo `ListilloLocal`) |
| GET | `/api/productos/{ean}` | sesión: producto (caché propia + Open Food Facts); 400 código malo, 404 nadie lo conoce, 503 OFF no responde |
| PUT | `/api/productos/{ean}` `{nombre, marca?, cantidad?}` | sesión: nombrar uno que OFF no conoce (409 si es de OFF) |
| POST | `/api/hogares/{h}/tickets/leer-pdf` (multipart `archivo`, ≤ 5 MB) | miembro: borrador sin guardar |
| POST | `/api/hogares/{h}/tickets/leer-texto` `{texto}` | miembro: borrador del texto de una foto |
| POST | `/api/hogares/{h}/tickets` | miembro: guarda, aprende y tacha (201, `{ticket, tachados}`) |
| GET | `/api/hogares/{h}/tickets` · `/{id}` | miembro: últimos 100 · uno con líneas |
| DELETE | `/api/hogares/{h}/tickets/{id}` | miembro (204) |
| GET | `/api/hogares/{h}/genericos` | miembro: catálogo con el mejor €/unidad de cada uno (clasifica antes lo pendiente) |
| GET | `/api/hogares/{h}/genericos/{id}` | miembro: `tiendas` (mejor de cada cadena, de barata a cara) y `productos` |
| POST | `/api/hogares/{h}/genericos` `{nombre, categoria, unidad}` | miembro: uno propio (201; 409 nombre repetido; 100 como mucho) |
| DELETE | `/api/hogares/{h}/genericos/{id}` | miembro: solo los propios (403 los del catálogo) |
| PUT | `/api/hogares/{h}/genericos/clasificacion` `{cadena, texto, genericoId?, tamano?}` | miembro: corrige un producto (sin `genericoId` = ninguno) (204) |
| GET | `/api/hogares/{h}/genericos/ticket/{ticketId}` | miembro: genérico, tamaño y €/unidad de cada línea |
| PUT | `/api/hogares/{h}/genericos/clasificacion-web` `{cadena, idExterno, genericoId?}` | miembro: corrige un producto de la web (común; solo genéricos de Listillo) (204) |
| GET | `/api/hogares/{h}/precios/buscar?q=fregona` | miembro: lo que se llama así en cada súper (`tiendas[]` con los 5 más baratos; `porUnidad` si todo se mide igual); 400 con menos de 3 letras |
| GET | `/api/hogares/{h}/lista/reparto` | miembro: lo pendiente repartido por súper (`grupos`, `sinPrecio`, `total`, `enUna`) |
| POST | `/api/admin/precios-web/importar[?cadena=LIDL]` | ROLE_ADMIN: importa ya las webs, todas o una (409 si está apagado; tarda minutos) |
| GET | `/api/hogares/{h}/gasto?mes=2026-10` | miembro: el mes (sin `mes`, el actual): `total` (tickets + anotados), `porTipo`, seis `meses` (hasta hoy si el elegido cabe), tiendas, pasillos y top |
| GET | `/api/hogares/{h}/gastos?mes=2026-10` · `/{id}` | miembro: los anotados del mes · uno |
| POST | `/api/hogares/{h}/gastos` `{concepto, importe, fecha, tipo, cadaMes}` | miembro (201; con `cadaMes`, además un gasto fijo ese día, hasta el 28) |
| PUT / DELETE | `/api/hogares/{h}/gastos/{id}` | miembro (cambiar; 204 al borrar) |
| GET | `/api/hogares/{h}/gastos/fijos` | miembro: los que se apuntan solos cada mes |
| DELETE | `/api/hogares/{h}/gastos/fijos/{id}` | miembro (204; lo ya apuntado se queda) |
| GET / PUT | `/api/yo/partes` `{partes: [LISTA, PRECIOS, GASTOS, GASOLINA]}` | sesión: qué partes usa cada persona (al menos una; sin elegir, todas) |
| GET | `/api/estaciones/cercanas` · `/{id}` · `/carburantes` · `/lugares?q=` · `/estado` | **público**: gasolineras del Ministerio (de Gasofa) |
| GET | `/api/estaciones/ruta?origenLatitud&origenLongitud&destinoLatitud&destinoLongitud&carburante&desvioKm` | **público** (Viajes, 1.2.0): ruta en coche con OSRM y las gasolineras a menos de `desvioKm` (2 por defecto, máx. 10) de la carretera, de la más barata a la más cara (`gasolina/viajes/`) |
| GET / POST / DELETE | `/api/favoritos[/{estacionId}]` | sesión: gasolineras favoritas de cada persona |
| GET / POST | `/api/hogares/{h}/vehiculos` · PUT / DELETE `/{id}` (archiva) · GET `/{id}/estadisticas` | miembro: coches de la casa, consumo y ahorro |
| GET / POST | `/api/hogares/{h}/repostajes` · PUT / DELETE `/{id}` | miembro: repostajes de la casa (cuentan en Gasto como GASOLINA) |
| GET | `/api/hogares` | sesión: mis hogares |
| POST | `/api/hogares` `{nombre}` | sesión (201; quien lo crea lo lleva) |
| GET | `/api/hogares/invitacion/{codigo}` | sesión: `{nombre, miembros, yaEstoyDentro}` para confirmar antes de entrar (404 si no existe) |
| POST | `/api/hogares/unirse` `{codigo}` | sesión (minúsculas, espacios y guiones valen; repetir no duplica) |
| GET / PATCH | `/api/hogares/{id}` `{nombre}` | miembro |
| POST | `/api/hogares/{id}/codigo` | propietario (el viejo deja de valer) |
| DELETE | `/api/hogares/{id}/miembros/yo` | miembro (204) |
| DELETE | `/api/hogares/{id}/miembros/{usuarioId}` | propietario (204) |
| GET / POST | `/api/hogares/{id}/lista` `{nombre, cantidad?}` | miembro |
| PATCH / DELETE | `/api/hogares/{id}/lista/{articulo}` `{nombre?, cantidad?, comprado?}` | miembro |
| DELETE | `/api/hogares/{id}/lista/comprados` | miembro |

**Gastos fijos**: no hay tarea programada. Antes de leer el gasto (`GET …/gasto` y
`GET …/gastos`) `GastosFijosAlDia` apunta los meses que ya han llegado a su día
(`GastoFijo.pendientes`), así no se pierde ninguno aunque el servidor estuviera parado. Va
en su propia transacción; si dos peticiones lo hacen a la vez, el índice único hace fallar
a la segunda y se ignora.

`/api/app/version` devuelve `estado`: `AL_DIA`, `OPCIONAL` (hay una nueva) u `OBLIGATORIA`
(la instalada es menor que `minima`). Una instalada mayor que la publicada cuenta como al
día. La mínima no puede ser mayor que la última (400).

**Hogares y lista**: quien no es miembro recibe **404** (no 403) para no confirmar que el
hogar existe. Límites: 5 hogares por persona, 12 personas por hogar, 300 cosas por lista
(409 al pasarse). Si se va el propietario, lo hereda el miembro más antiguo; si se va el
último, el hogar se borra con su lista. Apuntar algo que ya está pendiente con el mismo
nombre (sin mirar mayúsculas ni espacios) suma la cantidad. Todas las rutas de la lista
devuelven la lista entera ordenada (pendiente por orden de apuntado, comprado lo último
primero). Si dos cambian lo mismo a la vez, gana el último.

## Configuración

**Productos**: el código se valida con su dígito de control (EAN-8, UPC-A, EAN-13, GTIN-14)
antes de preguntar a nadie. La caché manda: lo de OFF se refresca a los 90 días, lo que
OFF no conocía se vuelve a preguntar a la semana y lo nombrado a mano se cruza con OFF al
mes (si OFF lo conoce ya, gana OFF). Sin `@Transactional` en `ProductoService`: la consulta
a OFF (hasta 4 s) no debe tener una transacción abierta. Tope propio de 12 consultas por
minuto (`LimitePorMinuto`, en memoria) por debajo de las 15 de OFF. Al apuntar con `ean`,
el mismo envase pendiente suma cantidad aunque el nombre sea otro.

**Tickets**: `ticket/lectura/InterpreteTicket` convierte texto (de PDF o de OCR) en
tienda (`Cadena`, por la cabecera), fecha, total y líneas con reglas comunes a los
tickets españoles (cantidad delante, precio unitario + importe, artículo a peso con el
peso en la línea siguiente, «2 x 0,99» debajo, letra de IVA al final, descuentos) y
corrige erratas típicas del OCR (O por 0, «EUR/KKE»). No inventa: si las líneas no suman
el total, avisa. `Casador` propone el producto: primero lo aprendido por el hogar en esa
cadena; si no, el candidato de la lista o de tickets anteriores que mejor encaja (el
ticket abrevia: «SEMI» casa con «semidesnatada»; al revés no). Al guardar se aprende y se
tacha de la lista lo pendiente con el mismo código o nombre.

Variables en `application.yml` (y `.env.example` en la raíz): `OFF_CONTACTO` (correo para
el User-Agent que pide Open Food Facts), `PIMON_SECURITY_JWT_SECRET`,
`GOOGLE_HABILITADO` + `GOOGLE_CLIENT_ID` (client-id de tipo **Aplicación web**),
`CREATE_DEFAULT_ADMIN` + `ADMIN_EMAIL` + `ADMIN_PASSWORD` (solo con la base vacía).

**Genéricos** (`generico/`): un producto es el **texto del ticket en su cadena** (como lo
aprendido de tickets), no el código de barras: la mayoría de líneas no lo llevan. Al guardar
un ticket (`TicketGuardado`, después del commit) y al abrir el catálogo, `Clasificador` pone
genérico a lo pendiente con `Adivinador`: primero la categoría de Open Food Facts más
concreta que sea de un genérico; si no, las palabras del nombre (palabra igual = 1,
abreviatura = 0,85, umbral 0,6; las palabras con cifras no cuentan). Lo que no adivina no se
guarda y se reintenta (p. ej. al crear un genérico propio). Lo corregido a mano (USUARIO) no
se toca nunca solo. `Tasador` calcula el €/unidad: tamaño corregido > peso de báscula >
cantidad de OFF > texto del ticket (`Medida`: «6X1L», «4X125G», «12 UDS», «docena») > 1 en
los genéricos por pieza. Cada producto cuenta con su **último** precio de los últimos 180
días. **Gasto** (`gasto/`): el total y las tiendas suman el total de cada ticket; pasillos y
genéricos suman líneas positivas; los descuentos van aparte.

**Precios de las webs** (`tienda/`). Un `CatalogoWeb` por cadena; cada uno
devuelve productos con precio, tamaño, unidad y €/unidad, y la sección de la web (para
saber en qué pasillos buscar el genérico):

| Cadena | Clase | De dónde | Notas |
|---|---|---|---|
| Mercadona | `MercadonaWeb` | API de su tienda (`/api/categories`) | ~4.500; €/u = `unit_price / unit_size` |
| Consum | `ConsumWeb` | API JSON (`/api/rest/V1.0/catalog/product`) | ~9.000; `centUnitAmount` por «1 Kg/1 L/100 ml» |
| Ahorramás | `AhorramasWeb` | HTML de sus secciones (SFCC), 48 por página | ~5.800; `data-gtm-layer` + «3,99€/LITRO» |
| Eroski | `EroskiWeb` | HTML de su buscador, por genérico (3 páginas) | ~3.300; envase en el nombre; 1,5 s de pausa y reintento si 429 |
| Caprabo | `CapraboWeb` | La misma tienda que Eroski | ~2.400 |
| Aldi | `AldiWeb` | `__NEXT_DATA__` de sus categorías (Algolia ya resuelto) | ~2.100; precio base €/kg |
| Lidl | `LidlWeb` | API de búsqueda (`q=*`, de 100 en 100) | ~160; casi todo bazar |

Bloqueadas (no se burla): Dia (Akamai), Carrefour (Cloudflare), Alcampo (AWS WAF),
El Corte Inglés/Hipercor (Akamai). **El bazar también se guarda** (Lidl salvo la ropa, el
bazar de Aldi, hogar de Eroski/Caprabo): lo de bazar sin envase cuenta como una pieza.
`BuscadorService` busca por palabras en `nombre_normalizado` en todas las cadenas. `PasillosPorTexto` saca los pasillos de las palabras
de la sección (Aldi, Consum); lo congelado solo busca en Congelados.

Detalles de la primera:
`MercadonaWeb` (`tienda.mercadona.es/api`, sin clave, almacén `MERCADONA_ALMACEN`, por
defecto `mad1`) y `LidlWeb` (`www.lidl.es/q/api/search`: busca cada genérico del catálogo y
se queda la alimentación con precio y envase legible; quita la marca de delante, que va en
mayúsculas, y la comida de mascotas; las ofertas llevan «oferta» en el formato). Dia y
Carrefour no: bloquean con Akamai y Cloudflare. `ImportadorPrecios` importa cada noche (`listillo.precios-web.cron`, 4:30)
y al arrancar si tiene más de 20 h, **solo con `PRECIOS_WEB=true`**. Lee fuera de
transacción, guarda en una por cadena, borra lo que ya no sale y, si la web no devuelve
nada, no toca lo que había. El €/unidad es `unit_price / unit_size` (por los rollos o
sobres de dentro si es un paquete). El genérico se adivina con `Adivinador.adivinarPorCabeza`
entre los del catálogo de Listillo de los pasillos de esa sección y con la misma unidad.
En Precios, en una cadena con web manda la web; si no, el último ticket.
**Un producto en cada súper** (`BuscadorService.enCadaSuper`, `GET
/api/hogares/{h}/precios/en-cada-super?cadena=X&id=Y`): palabras significativas del nombre
(`significativas`: sin cifras, sin envases ni unidades —`RUIDO`—, en singular); candidatos
con alguna de sus dos palabras más raras; en cada cadena visible, el de la misma unidad con
Jaccard **con pesos** ≥ 0,7 (`parecido`), prefiriendo el mismo tamaño (±15 %), luego el más
parecido y luego el más barato. El peso es el IDF de `RarezaDePalabras` (contado en memoria
sobre todos los nombres, cada 6 h): la marca es rara y pesa más, así que la Coca-Cola Zero
Zero no casa con la Pepsi Zero Zero aunque compartan cuatro palabras. Orden: €/unidad.

**Genéricos propios** (`GenericoService.deLaWeb`): los del catálogo toman lo de las webs
por la clasificación de cada producto (`generico_id`); los propios, buscando su nombre con
`ProductoTiendaRepository.buscar` (hasta cuatro palabras, singular, como el buscador), con
la misma unidad y sin lo descartado en `descartes_web` (V19). «No es esto» desde un propio
manda `quitarDe` y apunta el descarte para ese genérico; desde uno del catálogo sigue
corrigiendo el producto para todos.

Al arrancar, antes de importar, `reclasificar()` vuelve a adivinar el genérico de lo ya
importado con el catálogo de hoy (sin leer las webs; respeta lo corregido a mano): así una
migración que cambia pistas se nota en el despliegue y no a la noche siguiente.

**Open Prices**: `OpenPrices` busca los locales de España con precios
(`/api/v1/locations?osm_address_country__like=España|Espanya|Spain…`, la API de precios no
filtra por país), reconoce la cadena con `Cadena.detectar` sobre `osm_brand` u `osm_name` y
pide los precios de esos locales (`/api/v1/prices?location_id__in=…&date__gte=…&currency=EUR`).
`ImportadorOpenPrices` (5:15, y al arrancar si hace más de 20 h; `OPEN_PRICES`, encendido por
defecto) guarda en `productos_tienda` con `fuente = OPEN_PRICES` y `fecha_precio` (V17), solo
para las cadenas sin `CatalogoWeb` (todas si `PRECIOS_WEB` está apagado), el más reciente de
cada producto, y borra lo que ya no sale. Genérico: categoría de OFF y, si no, primera
palabra. En Precios, Open Prices solo gana a un ticket si es más reciente.
`POST /api/admin/open-prices/importar` (ROLE_ADMIN) lo importa ya. En los tests va apagado
(`ConPostgresEmbebido`) y `PreciosWebTest` usa un doble de `OpenPrices`.

**Reparto** (`reparto/`): cada cosa pendiente → su genérico (por los tickets con ese
nombre, por el producto escaneado o por el nombre) → la cadena con mejor €/unidad. Para
comparar tiendas, cada cosa se cuenta en todas con la misma cantidad (la del envase más
barato) a su €/unidad en cada una.

