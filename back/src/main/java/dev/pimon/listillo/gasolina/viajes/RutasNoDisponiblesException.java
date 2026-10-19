package dev.pimon.listillo.gasolina.viajes;

/** El servicio de rutas no contesta o contesta algo que no se entiende. */
public class RutasNoDisponiblesException extends RuntimeException {

    public RutasNoDisponiblesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
