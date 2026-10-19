package dev.pimon.listillo.gasolina.favoritos;

import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.gasolina.estaciones.Carburante;
import dev.pimon.listillo.gasolina.estaciones.CatalogoEstaciones;
import dev.pimon.listillo.gasolina.estaciones.Estacion;
import dev.pimon.listillo.gasolina.estaciones.EstacionMapper;
import dev.pimon.listillo.gasolina.estaciones.Geo;
import dev.pimon.listillo.gasolina.estaciones.dto.EstacionCercanaDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Alta, baja y consulta de gasolineras favoritas.
 *
 * Cada operación recibe el id del usuario autenticado y filtra por él. No hay ningún
 * endpoint que devuelva favoritos de otro: el id sale del JWT, nunca de la petición.
 */
@Slf4j
@Service
public class FavoritosService {

    private final FavoritoRepository  repositorio;
    private final CatalogoEstaciones  catalogo;

    public FavoritosService(FavoritoRepository repositorio, CatalogoEstaciones catalogo) {
        this.repositorio = repositorio;
        this.catalogo    = catalogo;
    }

    /**
     * Los favoritos del usuario, con los precios de ahora mismo.
     *
     * Si se pasan coordenadas, cada uno viene con su distancia; si no, con distancia null.
     *
     * Las estaciones que ya no están en el catálogo (una gasolinera que cierra desaparece
     * del listado del Ministerio) se omiten en lugar de devolverlas a medias: para el
     * usuario, un favorito sin precio ni horario es ruido. La fila se conserva en la base
     * porque el catálogo puede volver a traerla mañana.
     */
    @Transactional(readOnly = true)
    public List<EstacionCercanaDto> listar(String userId, Double latitud, Double longitud, Carburante carburante) {
        boolean hayUbicacion = latitud != null && longitud != null;

        return repositorio.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(favorito -> catalogo.porId(favorito.getEstacionId()))
                .flatMap(Optional::stream)
                .map(estacion -> EstacionMapper.aDto(
                        estacion,
                        hayUbicacion ? distancia(estacion, latitud, longitud) : null,
                        carburante))
                .toList();
    }

    /**
     * Marca una estación como favorita.
     *
     * Es idempotente: repetir la llamada devuelve el favorito que ya había en vez de dar
     * error. Es lo que quiere un botón de "me gusta" que se pulsa dos veces por un doble
     * clic o por un reintento del móvil con mala cobertura.
     */
    @Transactional
    public void anadir(String userId, String estacionId) {
        // Que exista en el catálogo se comprueba antes de guardar: si no, la base
        // acabaría llena de referencias a estaciones inventadas.
        if (catalogo.porId(estacionId).isEmpty()) {
            throw AppException.notFound("No hay ninguna estación con id " + estacionId);
        }
        if (repositorio.existsByUserIdAndEstacionId(userId, estacionId)) {
            return;
        }
        repositorio.save(new Favorito(userId, estacionId));
    }

    /**
     * Solo los ids, para que el buscador pueda poner las favoritas las primeras.
     *
     * No pasa por el catálogo ni construye DTOs: es una consulta que se hace en CADA
     * búsqueda de un usuario identificado, y lo único que necesita es el conjunto de ids.
     */
    @Transactional(readOnly = true)
    public Set<String> idsDe(String userId) {
        return repositorio.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Favorito::getEstacionId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public void eliminar(String userId, String estacionId) {
        Favorito favorito = repositorio.findByUserIdAndEstacionId(userId, estacionId)
                .orElseThrow(() -> AppException.notFound("Esa estación no está en tus favoritos"));
        repositorio.delete(favorito);
    }

    private double distancia(Estacion estacion, double latitud, double longitud) {
        return Geo.distanciaKm(latitud, longitud, estacion.latitud(), estacion.longitud());
    }
}
