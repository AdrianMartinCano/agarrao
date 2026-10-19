package dev.pimon.listillo.lista;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.hogar.AccesoHogar;
import dev.pimon.listillo.lista.dto.ArticuloDto;
import dev.pimon.listillo.lista.dto.CambiarArticuloRequest;
import dev.pimon.listillo.lista.dto.NuevoArticuloRequest;
import dev.pimon.listillo.producto.Ean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * La lista compartida de un hogar. Todo pasa por {@link AccesoHogar}: solo los miembros
 * la ven y la tocan. Si dos personas cambian lo mismo a la vez, gana el último (es una
 * lista de la compra, no un banco).
 */
@Service
@RequiredArgsConstructor
public class ListaService {

    /** Una compra enorme cabe de sobra; más es un error o un abuso. */
    static final int MAX_ARTICULOS = 300;

    /**
     * Arriba lo pendiente por orden de apuntado (así se lee como se escribió); abajo lo
     * comprado, lo último echado al carro primero.
     */
    private static final Comparator<ArticuloDto> ORDEN = Comparator
            .comparing(ArticuloDto::comprado)
            .thenComparing((a, b) -> a.comprado()
                    ? b.compradoEn().compareTo(a.compradoEn())
                    : a.creadoEn().compareTo(b.creadoEn()));

    private final ArticuloListaRepository articulos;
    private final AccesoHogar acceso;

    @Transactional(readOnly = true)
    public List<ArticuloDto> lista(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        return ordenada(hogarId);
    }

    /**
     * Apuntar algo que ya está pendiente con el mismo nombre suma cantidades en vez de
     * duplicar la línea: si dos personas apuntan «leche», sale «leche ×2».
     *
     * @return la lista entera, para que el móvil se ponga al día de paso
     */
    @Transactional
    public List<ArticuloDto> apuntar(UUID hogarId, String usuarioId, NuevoArticuloRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        String nombre = peticion.nombre().strip();
        int cantidad = peticion.cantidad() == null ? 1 : peticion.cantidad();

        String ean = peticion.ean();
        if (ean != null && !Ean.esValido(ean)) {
            throw AppException.badRequest("Ese código de barras no es válido: " + ean);
        }
        // El mismo envase, o lo mismo escrito a mano: suma en vez de repetir.
        List<ArticuloLista> iguales = ean != null
                ? articulos.findByHogarIdAndEanAndCompradoFalse(hogarId, ean)
                : List.of();
        if (iguales.isEmpty()) {
            iguales = articulos.pendientesLlamados(hogarId, nombre);
        }
        if (!iguales.isEmpty()) {
            iguales.getFirst().sumar(cantidad);
        } else {
            if (articulos.countByHogarId(hogarId) >= MAX_ARTICULOS) {
                throw AppException.conflict("La lista ya tiene " + MAX_ARTICULOS
                        + " cosas. Quita lo comprado para seguir apuntando");
            }
            articulos.save(new ArticuloLista(hogarId, nombre, cantidad, usuarioId, ean));
        }
        articulos.flush();
        return ordenada(hogarId);
    }

    @Transactional
    public List<ArticuloDto> cambiar(UUID hogarId, UUID articuloId, String usuarioId,
                                     CambiarArticuloRequest peticion) {
        acceso.exigirMiembro(hogarId, usuarioId);
        ArticuloLista articulo = articulos.findByIdAndHogarId(articuloId, hogarId)
                .orElseThrow(() -> AppException.notFound("Eso ya no está en la lista"));

        if (peticion.nombre() != null) {
            articulo.renombrar(peticion.nombre().strip());
        }
        if (peticion.cantidad() != null) {
            articulo.cambiarCantidad(peticion.cantidad());
        }
        if (peticion.comprado() != null) {
            articulo.marcarComprado(peticion.comprado(), usuarioId);
        }
        articulos.flush();
        return ordenada(hogarId);
    }

    /** Quitar algo que otro ya quitó no es un error: el resultado es el mismo. */
    @Transactional
    public List<ArticuloDto> quitar(UUID hogarId, UUID articuloId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        articulos.findByIdAndHogarId(articuloId, hogarId).ifPresent(articulos::delete);
        articulos.flush();
        return ordenada(hogarId);
    }

    @Transactional
    public List<ArticuloDto> quitarComprados(UUID hogarId, String usuarioId) {
        acceso.exigirMiembro(hogarId, usuarioId);
        articulos.borrarComprados(hogarId);
        return ordenada(hogarId);
    }

    private List<ArticuloDto> ordenada(UUID hogarId) {
        return articulos.listaDe(hogarId).stream().sorted(ORDEN).toList();
    }
}
