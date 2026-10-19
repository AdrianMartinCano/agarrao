/**
 * Leer la foto de un ticket en el propio móvil, sin mandarla a nadie: Tesseract compilado a
 * WebAssembly, en español y con el motor LSTM. Todo se sirve desde la app (`/tesseract/`):
 * el worker, el motor (uno de tres, según lo que soporte el navegador, ~4 MB) y el modelo
 * de español compacto (2 MB). La primera vez tarda; luego lo guarda el service worker.
 *
 * Antes de leer se limpia la foto: escala de grises, contraste y tamaño. El papel térmico
 * sale gris y arrugado, y eso es lo que más errores da.
 */
export async function leerFoto(foto: File, alProgresar: (avance: number) => void): Promise<string> {
  const imagen = await prepararImagen(foto);
  const { createWorker, OEM, PSM } = await import('tesseract.js');
  const worker = await createWorker('spa', OEM.LSTM_ONLY, {
    workerPath: '/tesseract/worker.min.js',
    corePath: '/tesseract/core',
    langPath: '/tesseract/lang',
    gzip: true,
    logger: (m: { status: string; progress: number }) => {
      if (m.status === 'recognizing text') alProgresar(m.progress);
    },
  });
  try {
    // Un ticket es una columna de líneas: «bloque único» lee mejor que el automático.
    await worker.setParameters({ tessedit_pageseg_mode: PSM.SINGLE_BLOCK, preserve_interword_spaces: '1' });
    const { data } = await worker.recognize(imagen);
    return data.text;
  } finally {
    await worker.terminate();
  }
}

/** Ancho al que se deja la foto: más no lee mejor y tarda más. */
const ANCHO = 1600;

async function prepararImagen(foto: File): Promise<HTMLCanvasElement> {
  const mapa = await createImageBitmap(foto);
  const escala = Math.min(1, ANCHO / mapa.width) || 1;
  const lienzo = document.createElement('canvas');
  lienzo.width = Math.round(mapa.width * escala);
  lienzo.height = Math.round(mapa.height * escala);
  const ctx = lienzo.getContext('2d', { willReadFrequently: true });
  if (!ctx) return lienzo;
  ctx.drawImage(mapa, 0, 0, lienzo.width, lienzo.height);
  mapa.close();

  const pixeles = ctx.getImageData(0, 0, lienzo.width, lienzo.height);
  const d = pixeles.data;
  // Gris + estirar el contraste entre el 2 % más oscuro y el 2 % más claro.
  const grises = new Uint8ClampedArray(d.length / 4);
  const histograma = new Uint32Array(256);
  for (let i = 0, j = 0; i < d.length; i += 4, j++) {
    const g = (d[i] * 299 + d[i + 1] * 587 + d[i + 2] * 114) / 1000;
    grises[j] = g;
    histograma[grises[j]]++;
  }
  const corte = grises.length * 0.02;
  let bajo = 0;
  for (let n = 0; bajo < 255 && n + histograma[bajo] < corte; bajo++) n += histograma[bajo];
  let alto = 255;
  for (let n = 0; alto > 0 && n + histograma[alto] < corte; alto--) n += histograma[alto];
  const rango = Math.max(1, alto - bajo);
  for (let i = 0, j = 0; i < d.length; i += 4, j++) {
    const v = Math.max(0, Math.min(255, ((grises[j] - bajo) * 255) / rango));
    d[i] = d[i + 1] = d[i + 2] = v;
  }
  ctx.putImageData(pixeles, 0, 0);
  return lienzo;
}
