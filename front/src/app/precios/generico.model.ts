import { Cadena, Unidad } from '../tickets/ticket.model';

export type Categoria =
  | 'FRUTA'
  | 'VERDURA'
  | 'CARNE'
  | 'PESCADO'
  | 'LACTEOS'
  | 'PANADERIA'
  | 'DESPENSA'
  | 'DESAYUNO'
  | 'BEBIDAS'
  | 'CONGELADOS'
  | 'LIMPIEZA'
  | 'HIGIENE'
  | 'HOGAR'
  | 'OTROS';

export type OrigenClasificacion = 'AUTO' | 'USUARIO';

/** De dónde sale un precio: un ticket del hogar o la web de la cadena. */
/** TICKET: vuestros tickets; WEB: la web del súper (de hoy); OPEN_PRICES: lo subió alguien a Open Prices. */
export type FuentePrecio = 'TICKET' | 'WEB' | 'OPEN_PRICES';

/** Los pasillos, en el orden en que se recorre un súper. */
export const CATEGORIAS: readonly Categoria[] = [
  'FRUTA',
  'VERDURA',
  'CARNE',
  'PESCADO',
  'LACTEOS',
  'PANADERIA',
  'DESPENSA',
  'DESAYUNO',
  'BEBIDAS',
  'CONGELADOS',
  'LIMPIEZA',
  'HIGIENE',
  'HOGAR',
  'OTROS',
];

export const NOMBRE_CATEGORIA: Record<Categoria, string> = {
  FRUTA: 'Fruta',
  VERDURA: 'Verdura',
  CARNE: 'Carne',
  PESCADO: 'Pescado',
  LACTEOS: 'Lácteos y huevos',
  PANADERIA: 'Pan',
  DESPENSA: 'Despensa',
  DESAYUNO: 'Desayuno y dulces',
  BEBIDAS: 'Bebidas',
  CONGELADOS: 'Congelados',
  LIMPIEZA: 'Limpieza',
  HIGIENE: 'Higiene',
  HOGAR: 'Hogar y bazar',
  OTROS: 'Otros',
};

/** El personaje de cada pasillo (public/personajes), para los genéricos sin dibujo propio. */
const RETRATO_CATEGORIA: Record<Categoria, string> = {
  FRUTA: 'fruta.svg',
  VERDURA: 'verdura.svg',
  CARNE: 'carne.svg',
  PESCADO: 'pescado.svg',
  LACTEOS: 'leche.svg',
  PANADERIA: 'pan.svg',
  DESPENSA: 'despensa.svg',
  DESAYUNO: 'desayuno.svg',
  BEBIDAS: 'bebidas.svg',
  CONGELADOS: 'congelados.svg',
  LIMPIEZA: 'limpieza.svg',
  HIGIENE: 'higiene.svg',
  HOGAR: 'hogar.svg',
  OTROS: 'listillo.svg',
};

export function retrato(g: { retrato: string | null; categoria: Categoria | null }): string {
  return `personajes/${g.retrato ?? RETRATO_CATEGORIA[g.categoria ?? 'OTROS']}`;
}

/** «€/L», «€/kg», «€/ud». */
export function porUnidad(unidad: Unidad): string {
  return unidad === 'KG' ? '€/kg' : unidad === 'L' ? '€/L' : '€/ud';
}

/** «0,89 €/L». Con tres decimales si no llega al céntimo (0,004 €/ud no es 0,00). */
export function precioPorUnidad(precio: number, unidad: Unidad): string {
  const decimales = precio < 0.1 ? 3 : 2;
  return `${precio.toLocaleString('es-ES', { minimumFractionDigits: decimales, maximumFractionDigits: decimales })} ${porUnidad(unidad)}`;
}

/**
 * Un nombre de ticket se escribe como se dice: «LECHE SEMI HACEND 6X1L» → «Leche semi hacend
 * 6x1l». Lo que ya viene con minúsculas (Open Food Facts, la lista) se deja como está.
 */
export function comoSeDice(nombre: string): string {
  if (nombre !== nombre.toUpperCase()) return nombre;
  const minusculas = nombre.toLocaleLowerCase('es-ES');
  return minusculas.charAt(0).toLocaleUpperCase('es-ES') + minusculas.slice(1);
}

/** «0,06 €/L»: la diferencia entre dos precios, en céntimos si llega. */
export function diferenciaPorUnidad(dif: number, unidad: Unidad): string {
  const decimales = dif < 0.01 ? 3 : 2;
  return `${dif.toLocaleString('es-ES', { minimumFractionDigits: decimales, maximumFractionDigits: decimales })} ${porUnidad(unidad)}`;
}

/** «6 L», «0,5 kg», «12 ud». */
export function tamano(valor: number, unidad: Unidad): string {
  const u = unidad === 'KG' ? 'kg' : unidad === 'L' ? 'L' : 'ud';
  return `${valor.toLocaleString('es-ES', { maximumFractionDigits: 3 })} ${u}`;
}

export interface GenericoResumen {
  id: string;
  nombre: string;
  categoria: Categoria;
  unidad: Unidad;
  retrato: string | null;
  propio: boolean;
  /** Null: todavía sin precio (la ficha sale en gris). */
  mejorPrecio: number | null;
  mejorCadena: Cadena | null;
  mejorFuente: FuentePrecio | null;
  /** Comprados por el hogar. */
  productos: number;
  /** En las webs de los súper. */
  productosWeb: number;
  ultimaCompra: string | null;
}

/** Lo más barato de un genérico en una cadena (de su web si la hay; si no, de un ticket). */
export interface PrecioTienda {
  cadena: Cadena;
  /** Por unidad del genérico. */
  precio: number;
  fecha: string;
  fuente: FuentePrecio;
  nombre: string;
  tamano: number | null;
  /** Lo que cuesta el envase (o lo que se pagó). */
  importe: number;
  /** La ficha en la web de la cadena (solo si sale de la web). */
  url: string | null;
  imagenUrl: string | null;
}

/** Un producto de la web de una cadena que es este genérico. */
export interface ProductoWeb {
  cadena: Cadena;
  idExterno: string;
  nombre: string;
  formato: string | null;
  precio: number;
  tamano: number;
  precioUnidad: number;
  imagenUrl: string | null;
  url: string | null;
  /** El día del precio. */
  actualizado: string;
  fuente: FuentePrecio;
}

export interface ProductoDeGenerico {
  cadena: Cadena;
  /** Como lo pone el ticket: identifica el producto para corregirlo. */
  texto: string;
  nombre: string;
  marca: string | null;
  tamano: number | null;
  precioUnidad: number | null;
  importe: number;
  fecha: string;
  compras: number;
  origen: OrigenClasificacion;
}

export interface GenericoDetalle {
  id: string;
  nombre: string;
  categoria: Categoria;
  unidad: Unidad;
  retrato: string | null;
  propio: boolean;
  tiendas: PrecioTienda[];
  productos: ProductoDeGenerico[];
  enLaWeb: ProductoWeb[];
}

/** Lo encontrado al buscar en las webs de los súper, por cadena (la más barata primero). */
export interface Busqueda {
  texto: string;
  /** true: comparado por precio por unidad (todo se mide igual); false: por precio del envase. */
  porUnidad: boolean;
  tiendas: {
    cadena: Cadena;
    encontrados: number;
    productos: ProductoBuscado[];
  }[];
}

/** Un producto de la web de un súper, tal como sale al buscar. */
export interface ProductoBuscado {
  idExterno: string;
  nombre: string;
  formato: string | null;
  precio: number;
  tamano: number;
  unidad: Unidad;
  precioUnidad: number;
  imagenUrl: string | null;
  url: string | null;
  /** El día del precio. */
  fecha: string;
  fuente: FuentePrecio;
}

/** Un producto concreto y ese mismo producto en cada súper, del más barato al más caro por unidad. */
export interface EnCadaSuper {
  cadena: Cadena;
  elegido: ProductoBuscado;
  tiendas: {
    cadena: Cadena;
    producto: ProductoBuscado;
    /** false: en ese súper no está del mismo tamaño y se compara por precio por unidad. */
    mismoTamano: boolean;
  }[];
}

/** «12 mar» o, si no es de este año, «12 mar 2025». */
export function diaCorto(fecha: string): string {
  const d = new Date(`${fecha}T12:00:00`);
  const opciones: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };
  if (d.getFullYear() !== new Date().getFullYear()) opciones.year = 'numeric';
  return d.toLocaleDateString('es-ES', opciones);
}

export interface Generico {
  id: string;
  nombre: string;
  categoria: Categoria;
  unidad: Unidad;
  retrato: string | null;
  propio: boolean;
}

export interface ClasificacionLinea {
  lineaId: string;
  genericoId: string | null;
  generico: string | null;
  categoria: Categoria | null;
  unidad: Unidad | null;
  retrato: string | null;
  tamano: number | null;
  precioUnidad: number | null;
  origen: OrigenClasificacion | null;
}

export interface Clasificar {
  cadena: Cadena;
  texto: string;
  /** Null: no es ningún genérico. */
  genericoId: string | null;
  tamano: number | null;
}
