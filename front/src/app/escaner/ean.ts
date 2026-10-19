/**
 * El dígito de control de EAN-8, UPC-A, EAN-13 y GTIN-14, como en el back. Una lectura
 * mala de la cámara casi nunca lo cuadra: filtra lecturas falsas antes de preguntar.
 */
export function eanValido(codigo: string): boolean {
  if (!/^(\d{8}|\d{12,14})$/.test(codigo)) return false;
  const digitos = [...codigo].map(Number);
  const control = digitos.pop() ?? -1;
  const suma = digitos.reverse().reduce((total, d, i) => total + d * (i % 2 === 0 ? 3 : 1), 0);
  return (10 - (suma % 10)) % 10 === control;
}
