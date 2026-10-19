import { Cadena } from '../tickets/ticket.model';

export type RolMiembro = 'PROPIETARIO' | 'MIEMBRO';

/** Un hogar en «mis hogares». */
export interface HogarResumen {
  id: string;
  nombre: string;
  miRol: RolMiembro;
  miembros: number;
}

export interface Miembro {
  usuarioId: string;
  nombre: string;
  rol: RolMiembro;
  unidoEn: string;
}

/** A qué hogar lleva un código, antes de entrar. */
export interface Invitacion {
  nombre: string;
  miembros: number;
  yaEstoyDentro: boolean;
}

/** El hogar entero: quién está y el código para invitar. */
export interface Hogar {
  id: string;
  nombre: string;
  codigoInvitacion: string;
  miRol: RolMiembro;
  miembros: Miembro[];
  /** Los supermercados que la casa ve y compara (Ajustes). */
  cadenas: Cadena[];
}
