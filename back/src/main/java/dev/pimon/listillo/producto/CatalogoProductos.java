package dev.pimon.listillo.producto;

import java.util.Optional;

/**
 * Un catálogo externo de productos por código de barras. Hoy es Open Food Facts; detrás de
 * esta interfaz para poder probar sin red y cambiarlo sin tocar el servicio.
 */
public interface CatalogoProductos {

    /**
     * @return los datos, o vacío si el catálogo no conoce el código
     * @throws CatalogoNoDisponible si no responde, tarda demasiado o se ha pasado del límite
     */
    Optional<DatosProducto> buscar(String ean);

    class CatalogoNoDisponible extends RuntimeException {
        public CatalogoNoDisponible(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
