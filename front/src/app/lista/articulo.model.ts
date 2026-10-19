export interface Articulo {
  id: string;
  nombre: string;
  cantidad: number;
  comprado: boolean;
  /** Null si quien lo apuntó ya borró su cuenta. */
  apuntadoPor: string | null;
  apuntadoPorId: string;
  /** Si se apuntó escaneando. */
  ean: string | null;
  creadoEn: string;
  compradoEn: string | null;
}
