package dev.pimon.listillo.generico;

import dev.pimon.listillo.producto.Producto;
import dev.pimon.listillo.producto.ProductoRepository;
import dev.pimon.listillo.ticket.TicketGuardado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pone genérico a los productos de los tickets de un hogar que todavía no lo tienen. Lo que
 * no sabe adivinar se queda sin fila y se vuelve a intentar la próxima vez (por ejemplo,
 * cuando el hogar crea un genérico nuevo).
 */
@Service
@RequiredArgsConstructor
public class Clasificador {

    /** Todos los tickets del hogar: la app empezó en 2026. */
    static final LocalDate DESDE_SIEMPRE = LocalDate.of(2000, 1, 1);

    private final ClasificacionRepository clasificaciones;
    private final GenericoRepository genericos;
    private final ProductoRepository productos;

    /** Después de guardar un ticket, ya fuera de su transacción: si falla, el ticket queda guardado. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alGuardarTicket(TicketGuardado evento) {
        clasificarPendientes(evento.hogarId());
    }

    @Transactional
    public void clasificarPendientes(UUID hogarId) {
        Set<Clasificacion.Clave> hechas = clasificaciones.findByClaveHogarId(hogarId).stream()
                .map(Clasificacion::getClave).collect(Collectors.toSet());
        // La compra más reciente de cada producto pendiente (vienen de la más nueva a la más vieja).
        Map<Clasificacion.Clave, Compra> pendientes = new LinkedHashMap<>();
        for (Compra c : clasificaciones.compras(hogarId, DESDE_SIEMPRE)) {
            Clasificacion.Clave clave = c.clave(hogarId);
            if (!clave.getTexto().isEmpty() && !hechas.contains(clave)) {
                pendientes.putIfAbsent(clave, c);
            }
        }
        if (pendientes.isEmpty()) {
            return;
        }
        List<Generico> candidatos = genericos.delHogar(hogarId);
        Map<String, Producto> porEan = productos(pendientes.values().stream().map(Compra::ean).toList());

        List<Clasificacion> nuevas = new ArrayList<>();
        pendientes.forEach((clave, compra) -> {
            Producto p = compra.ean() == null ? null : porEan.get(compra.ean());
            List<String> nombres = Arrays.asList(p == null ? null : p.getNombre(), compra.nombre(), compra.texto());
            Adivinador.adivinar(p == null ? null : p.getCategorias(), nombres, candidatos)
                    .ifPresent(g -> nuevas.add(Clasificacion.adivinada(clave, g.getId())));
        });
        clasificaciones.saveAll(nuevas);
    }

    /** Los productos de estos códigos que haya en la caché. Admite get(null): las líneas sin código. */
    Map<String, Producto> productos(List<String> eans) {
        List<String> conEan = eans.stream().filter(Objects::nonNull).distinct().toList();
        Map<String, Producto> porEan = new HashMap<>();
        if (!conEan.isEmpty()) {
            productos.findAllById(conEan).forEach(p -> porEan.put(p.getEan(), p));
        }
        return porEan;
    }
}
