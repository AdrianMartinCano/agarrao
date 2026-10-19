export type OrigenProducto = 'OPEN_FOOD_FACTS' | 'USUARIO' | 'DESCONOCIDO';

export interface Producto {
  ean: string;
  nombre: string;
  marca: string | null;
  cantidad: string | null;
  imagenUrl: string | null;
  origen: OrigenProducto;
  /** Lo que hay que citar al enseñarlo («Open Food Facts»), o null. */
  fuente: string | null;
}

export interface DatosProducto {
  nombre: string;
  marca?: string;
  cantidad?: string;
}
