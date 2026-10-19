package dev.pimon.listillo.generico;

import dev.pimon.listillo.producto.Producto;
import dev.pimon.listillo.ticket.Unidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * El precio por unidad de una compra: lo pagado entre lo que trae, en la unidad del
 * genérico (€/L, €/kg, €/ud).
 *
 * Lo que trae cada unidad comprada sale, por este orden, de:
 * <ol>
 *   <li>lo que corrigió una persona;</li>
 *   <li>el peso de la báscula, si la línea va a peso («0,850 kg × 1,99 €/kg»);</li>
 *   <li>la cantidad de Open Food Facts del producto («6 x 1 l»);</li>
 *   <li>el texto del ticket («LECHE SEMI 6X1L») o el nombre de la lista;</li>
 *   <li>en un genérico por piezas, 1 pieza.</li>
 * </ol>
 */
final class Tasador {

    private Tasador() {
    }

    /** Lo que trae UNA unidad comprada, en la unidad del genérico. Vacío si no se sabe. */
    static Optional<BigDecimal> tamano(Compra compra, Clasificacion clasificacion, Producto producto, Generico generico) {
        Unidad destino = generico.getUnidad();
        if (clasificacion != null && clasificacion.getTamano() != null) {
            return Optional.of(clasificacion.getTamano());
        }
        if (compra.unidad() != Unidad.UD) {
            // A peso: la cantidad ya va en kilos o litros.
            return compra.unidad() == destino ? Optional.of(BigDecimal.ONE) : Optional.empty();
        }
        Optional<BigDecimal> leido = Optional.empty();
        if (producto != null) {
            leido = Medida.leer(producto.getCantidad()).flatMap(m -> m.en(destino));
        }
        leido = leido.or(() -> Medida.leer(compra.texto()).flatMap(m -> m.en(destino)))
                .or(() -> Medida.leer(compra.nombre()).flatMap(m -> m.en(destino)));
        if (leido.isEmpty() && destino == Unidad.UD) {
            return Optional.of(BigDecimal.ONE);
        }
        return leido;
    }

    /** Euros por unidad del genérico. Vacío en descuentos, devoluciones o sin tamaño. */
    static Optional<BigDecimal> porUnidad(Compra compra, BigDecimal tamano) {
        if (tamano == null || compra.importe().signum() <= 0 || compra.cantidad().signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal cuanto = compra.cantidad().multiply(tamano);
        return Optional.of(compra.importe().divide(cuanto, 3, RoundingMode.HALF_UP));
    }
}
