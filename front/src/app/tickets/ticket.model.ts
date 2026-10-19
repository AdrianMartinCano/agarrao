export type Cadena =
  | 'MERCADONA' | 'LIDL' | 'CARREFOUR' | 'DIA' | 'ALCAMPO' | 'EROSKI' | 'ALDI' | 'CONSUM'
  | 'AHORRAMAS' | 'EL_CORTE_INGLES' | 'GADIS' | 'BONPREU' | 'CAPRABO' | 'FAMILY_CASH' | 'COSTCO' | 'OTRA';
export type Unidad = 'UD' | 'KG' | 'L';
export type OrigenTicket = 'PDF' | 'FOTO';

export const NOMBRE_CADENA: Record<Cadena, string> = {
  MERCADONA: 'Mercadona', LIDL: 'Lidl', CARREFOUR: 'Carrefour', DIA: 'Dia', ALCAMPO: 'Alcampo',
  EROSKI: 'Eroski', ALDI: 'Aldi', CONSUM: 'Consum', AHORRAMAS: 'Ahorramás',
  EL_CORTE_INGLES: 'El Corte Inglés', GADIS: 'Gadis', BONPREU: 'Bonpreu', CAPRABO: 'Caprabo', FAMILY_CASH: 'Family Cash', COSTCO: 'Costco',
  OTRA: 'Otra tienda',
};

export interface Sugerencia {
  ean: string | null;
  nombre: string;
  /** APRENDIDO: ya se casó así en esta cadena. PARECIDO: se parece a algo de tu lista. */
  origen: 'APRENDIDO' | 'PARECIDO';
}

export interface LineaBorrador {
  texto: string;
  cantidad: number;
  unidad: Unidad;
  precioUnitario: number | null;
  importe: number;
  sugerencia: Sugerencia | null;
}

export interface Borrador {
  cadena: Cadena;
  tiendaTexto: string | null;
  fecha: string | null;
  total: number | null;
  origen: OrigenTicket;
  lineas: LineaBorrador[];
  avisos: string[];
  texto: string;
}

export interface LineaTicket {
  id: string;
  texto: string;
  cantidad: number;
  unidad: Unidad;
  precioUnitario: number | null;
  importe: number;
  ean: string | null;
  nombre: string | null;
}

export interface Ticket {
  id: string;
  cadena: Cadena;
  tiendaTexto: string | null;
  fecha: string;
  total: number;
  origen: OrigenTicket;
  lineas: LineaTicket[];
}

export interface TicketResumen {
  id: string;
  cadena: Cadena;
  fecha: string;
  total: number;
  lineas: number;
  casadas: number;
}

export interface LineaAGuardar {
  texto: string;
  cantidad: number;
  unidad: Unidad;
  precioUnitario: number | null;
  importe: number;
  ean: string | null;
  nombre: string | null;
}

export interface TicketAGuardar {
  cadena: Cadena;
  tiendaTexto: string | null;
  fecha: string;
  total: number | null;
  origen: OrigenTicket;
  lineas: LineaAGuardar[];
}

export interface Guardado {
  ticket: Ticket;
  tachados: number;
}
