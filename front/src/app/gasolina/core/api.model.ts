/**
 * Tipos que devuelve la API. Están escritos a mano y a propósito: son el contrato con
 * el backend y cualquier cambio allí debe romper la compilación aquí, no fallar en
 * tiempo de ejecución.
 */

/** Sobre estándar de todas las respuestas del backend (ApiResponse<T> en Java). */
export interface RespuestaApi<T> {
  success: boolean;
  message?: string;
  /** Código estable de error, p. ej. 'CATALOGO_NO_DISPONIBLE'. */
  code?: string;
  data: T;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  total: number;
  totalPaginas: number;
}

/**
 * Cómo está el precio en la zona buscada.
 *
 * Lo calcula el backend sobre TODAS las encontradas y no sobre la página, porque una
 * media que cambiara al pasar de página no serviría de referencia para nada.
 */
export interface ResumenPrecios {
  medio: number;
  minimo: number;
  /** Cuántas se han promediado: con dos, una media no dice gran cosa. */
  estaciones: number;
}

export interface BusquedaCercanas {
  estaciones: Pagina<EstacionCercana>;
  /** Null sin carburante elegido: no hay un precio único que promediar. */
  resumen: ResumenPrecios | null;
}

export interface EstacionCercana {
  id: string;
  rotulo: string;
  direccion: string;
  municipio: string;
  provincia: string;
  codigoPostal: string;
  horario: string;
  latitud: number;
  longitud: number;
  /** En línea recta. Null al listar favoritos sin ubicación. */
  distanciaKm: number | null;
  /** Precio del carburante buscado. Null si la búsqueda no filtró por uno. */
  precio: number | null;
  /** Todos los precios de la estación, por clave de carburante. */
  precios: Record<string, number>;
}

export interface Carburante {
  clave: string;
  etiqueta: string;
}

/** Un municipio o código postal desde el que se puede buscar. */
export interface Lugar {
  nombre: string;
  detalle: string;
  tipo: 'MUNICIPIO' | 'CODIGO_POSTAL';
  latitud: number;
  longitud: number;
  estaciones: number;
}

export interface EstadoCatalogo {
  disponible: boolean;
  estaciones: number;
  ultimaActualizacion: string | null;
}

export type OrdenBusqueda = 'DISTANCIA' | 'PRECIO';

export interface Coordenadas {
  latitud: number;
  longitud: number;
}

export interface FiltrosBusqueda {
  carburante: string | null;
  radioKm: number;
  orden: OrdenBusqueda;
}

/**
 * Si hay login con Google y con qué identificador.
 *
 * El client_id lo manda el backend en lugar de estar escrito aquí: hay uno distinto en
 * local y en producción, y es además el que el backend valida contra el `aud` del token.
 * Tenerlo en un solo sitio evita el clásico "en mi máquina funciona".
 */
export interface ConfigGoogle {
  habilitado: boolean;
  clientId: string;
}

/** Datos de la sesión que devuelve /api/auth/login. */
export interface Sesion {
  token: string;
  /** Milisegundos de validez del token, para saber cuándo caduca. */
  expiresIn: number;
  id: string;
  email: string;
  roles: string[];
}

/** Viajes: una gasolinera de camino (ver GasolinerasEnRuta.java). */
export interface GasolineraEnRuta {
  estacion: EstacionCercana;
  /** En qué kilómetro del viaje queda. */
  kmDesdeOrigen: number;
  /** A cuánto está de la carretera, en línea recta. */
  desvioKm: number;
}

export interface Viaje {
  distanciaKm: number;
  duracionMin: number;
  desvioKm: number;
  /** La línea de la ruta, [latitud, longitud], para pintarla. */
  ruta: [number, number][];
  /** Cuántas hay de camino (aunque `estaciones` traiga solo las más baratas). */
  enRuta: number;
  mediaRuta: number | null;
  minimaRuta: number | null;
  estaciones: GasolineraEnRuta[];
}
