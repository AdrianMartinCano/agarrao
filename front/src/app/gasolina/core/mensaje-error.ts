/**
 * Saca un mensaje legible del error que devuelve la API.
 *
 * El backend contesta siempre con el mismo sobre ({@code ApiResponse}), así que su
 * `message` ya viene escrito en español y pensado para el usuario ("Email o contraseña
 * incorrectos"). Merece la pena aprovecharlo en vez de enseñar un genérico. Si el error
 * es de red y no hay cuerpo, se recurre al mensaje de respaldo.
 */
export function mensajeDeErrorHttp(error: unknown, porDefecto: string): string {
  if (typeof error !== 'object' || error === null || !('error' in error)) {
    return porDefecto;
  }

  const cuerpo = (error as { error?: unknown }).error;
  if (typeof cuerpo === 'object' && cuerpo !== null && 'message' in cuerpo) {
    const mensaje = (cuerpo as { message?: unknown }).message;
    if (typeof mensaje === 'string' && mensaje.trim()) {
      return mensaje;
    }
  }

  return porDefecto;
}
