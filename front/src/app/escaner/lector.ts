/** Los formatos de envase de supermercado. UPC-E (6 dígitos) no: el back no lo admite. */
const FORMATOS = ['ean_13', 'ean_8', 'upc_a'];

/** Lo mínimo de la Barcode Detection API que se usa (TypeScript no la trae). */
interface Detector {
  detect(fuente: HTMLVideoElement): Promise<{ rawValue: string }[]>;
}

interface ConstructorDetector {
  new (opciones: { formats: string[] }): Detector;
  getSupportedFormats(): Promise<string[]>;
}

export interface Lector {
  /** Los códigos que se ven ahora mismo en el vídeo. */
  leer(video: HTMLVideoElement): Promise<string[]>;
  /** Para saber en pruebas y en la documentación cuál se está usando. */
  readonly nativo: boolean;
}

/**
 * El lector del propio navegador si existe y lee EAN (Chrome en Android); si no (Safari,
 * Firefox), `barcode-detector`, que trae zxing compilado a WebAssembly. Se carga solo aquí,
 * y su .wasm (1 MB) se sirve desde la app (`/zxing/`), no desde una CDN: así funciona sin
 * conexión una vez visto y no depende de terceros.
 */
export async function crearLector(): Promise<Lector> {
  const propio = (globalThis as { BarcodeDetector?: ConstructorDetector }).BarcodeDetector;
  if (propio) {
    try {
      const soportados = await propio.getSupportedFormats();
      if (FORMATOS.every((f) => soportados.includes(f))) {
        return envolver(new propio({ formats: FORMATOS }), true);
      }
    } catch {
      // Algunos navegadores lo declaran y luego fallan: a la versión de respaldo.
    }
  }
  const { BarcodeDetector, prepareZXingModule } = await import('barcode-detector/ponyfill');
  prepareZXingModule({
    overrides: {
      locateFile: (ruta: string, prefijo: string) => (ruta.endsWith('.wasm') ? `/zxing/${ruta}` : prefijo + ruta),
    },
  });
  return envolver(new BarcodeDetector({ formats: FORMATOS as never[] }), false);
}

function envolver(detector: Detector, nativo: boolean): Lector {
  return {
    nativo,
    leer: async (video) => (await detector.detect(video)).map((c) => c.rawValue),
  };
}
