import { Miembro } from './hogar.model';

/**
 * Cada persona del hogar lleva un color de cinta, por orden de llegada: el mismo en la
 * pantalla del hogar y en las filas de la lista que apuntó. Seis colores: hasta la sexta
 * persona no se repite ninguno. El orden alterna cálidos y fríos para que dos seguidos
 * nunca se parezcan (el rosa y el coral, los más cercanos, quedan en las puntas).
 */
const CINTAS = ['listillo', 'leche', 'huevo', 'lechuga', 'uva', 'tomate'] as const;

export function colorDeCinta(indice: number): string {
  return `var(--cinta-${CINTAS[indice % CINTAS.length]})`;
}

/** Quien ya no está en el hogar (o borró su cuenta) lleva una cinta lila neutra. */
export const CINTA_DE_FUERA = 'var(--placa-nombre-lila)';

export function inicial(nombre: string | null | undefined): string {
  return nombre?.trim().charAt(0).toLocaleUpperCase('es') || '?';
}

/** id de usuario → su color, según el orden de los miembros. */
export function cintasPorPersona(miembros: readonly Miembro[]): ReadonlyMap<string, string> {
  return new Map(miembros.map((m, i) => [m.usuarioId, colorDeCinta(i)]));
}
