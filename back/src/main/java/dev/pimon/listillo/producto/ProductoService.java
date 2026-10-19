package dev.pimon.listillo.producto;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.producto.dto.NombrarProductoRequest;
import dev.pimon.listillo.producto.dto.ProductoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Qué producto hay detrás de un código de barras: primero la caché propia y, si no sabe o
 * lo que sabe es viejo, Open Food Facts.
 *
 * Sin {@code @Transactional} a propósito: la consulta a OFF puede tardar segundos y no debe
 * tener una transacción abierta mientras tanto. Cada lectura y escritura del repositorio
 * va en la suya.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    /** Lo de OFF cambia poco (nombre, marca): se refresca cada tres meses. */
    static final Duration FRESCO_OFF = Duration.ofDays(90);
    /** Un código que OFF no conocía se vuelve a preguntar a la semana: alguien puede añadirlo. */
    static final Duration FRESCO_DESCONOCIDO = Duration.ofDays(7);
    /** Lo nombrado a mano se cruza con OFF cada mes: si OFF lo conoce ya, manda OFF. */
    static final Duration FRESCO_USUARIO = Duration.ofDays(30);

    private final ProductoRepository productos;
    private final CatalogoProductos catalogo;

    /**
     * @throws AppException 400 si el código no es válido, 404 si nadie lo conoce, 503 si OFF
     *                      no responde y no hay nada guardado
     */
    public ProductoDto buscar(String ean) {
        exigirValido(ean);
        Optional<Producto> guardado = productos.findById(ean);
        if (guardado.isPresent() && fresco(guardado.get())) {
            return aDtoOExigirConocido(guardado.get());
        }

        Optional<DatosProducto> deOff;
        try {
            deOff = catalogo.buscar(ean);
        } catch (CatalogoProductos.CatalogoNoDisponible e) {
            // Mejor algo viejo que nada; sin nada guardado, que la app ofrezca escribirlo.
            if (guardado.isPresent()) {
                return aDtoOExigirConocido(guardado.get());
            }
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOGO_NO_DISPONIBLE",
                    "Ahora mismo no podemos buscarlo. Escribe qué es y seguimos");
        }

        Producto producto;
        if (deOff.isPresent()) {
            producto = guardado.orElseGet(() -> Producto.deOpenFoodFacts(ean, deOff.get()));
            producto.actualizarDeOpenFoodFacts(deOff.get());
        } else {
            producto = guardado.orElseGet(() -> Producto.desconocido(ean));
            producto.marcarConsultado();
        }
        return aDtoOExigirConocido(productos.save(producto));
    }

    /**
     * Ponerle nombre a un código que OFF no conoce. Lo que viene de OFF no se pisa: si
     * alguien lo pudiera cambiar, cambiaría para todos los hogares.
     */
    public ProductoDto nombrar(String ean, NombrarProductoRequest peticion, String usuarioId) {
        exigirValido(ean);
        Optional<Producto> guardado = productos.findById(ean);
        if (guardado.isPresent() && guardado.get().getOrigen() == OrigenProducto.OPEN_FOOD_FACTS) {
            throw AppException.conflict("Ese producto ya lo conocemos: " + guardado.get().getNombre());
        }
        String nombre = peticion.nombre().strip();
        String marca = vacioANull(peticion.marca());
        String cantidad = vacioANull(peticion.cantidad());
        Producto producto = guardado
                .map(p -> {
                    p.nombrar(nombre, marca, cantidad, usuarioId);
                    return p;
                })
                .orElseGet(() -> Producto.nombrado(ean, nombre, marca, cantidad, usuarioId));
        return aDto(productos.save(producto));
    }

    private static boolean fresco(Producto p) {
        Duration vida = switch (p.getOrigen()) {
            case OPEN_FOOD_FACTS -> FRESCO_OFF;
            case DESCONOCIDO -> FRESCO_DESCONOCIDO;
            case USUARIO -> FRESCO_USUARIO;
        };
        return p.getConsultadoEn().isAfter(LocalDateTime.now().minus(vida));
    }

    private static void exigirValido(String ean) {
        if (!Ean.esValido(ean)) {
            throw AppException.badRequest("Ese código de barras no es válido: " + ean);
        }
    }

    private static ProductoDto aDtoOExigirConocido(Producto p) {
        if (p.getOrigen() == OrigenProducto.DESCONOCIDO) {
            throw AppException.notFound("No conocemos este código todavía. Escribe qué es");
        }
        return aDto(p);
    }

    private static ProductoDto aDto(Producto p) {
        return new ProductoDto(p.getEan(), p.getNombre(), p.getMarca(), p.getCantidad(), p.getImagenUrl(),
                p.getOrigen(), p.getOrigen() == OrigenProducto.OPEN_FOOD_FACTS ? "Open Food Facts" : null);
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
