package dev.pimon.listillo.lista;

import dev.pimon.listillo.lista.dto.ArticuloDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La lista no se pagina: tiene como mucho {@link ListaService#MAX_ARTICULOS} líneas y se
 * enseña entera de una vez (en el súper se mira toda).
 */
public interface ArticuloListaRepository extends JpaRepository<ArticuloLista, UUID> {

    /** Con el nombre de quien lo apuntó. LEFT JOIN: si borró su cuenta, el artículo sigue. */
    @Query("""
            select new dev.pimon.listillo.lista.dto.ArticuloDto(
                a.id, a.nombre, a.cantidad, a.comprado, u.name, a.creadoPor, a.ean, a.creadoEn, a.compradoEn)
            from ArticuloLista a left join dev.pimon.users.entity.AppUser u on u.id = a.creadoPor
            where a.hogarId = :hogarId
            """)
    List<ArticuloDto> listaDe(@Param("hogarId") UUID hogarId);

    Optional<ArticuloLista> findByIdAndHogarId(UUID id, UUID hogarId);

    /** El mismo nombre aún sin comprar, sin mirar mayúsculas ni espacios. */
    @Query("""
            select a from ArticuloLista a
            where a.hogarId = :hogarId and a.comprado = false
              and lower(trim(a.nombre)) = lower(trim(:nombre))
            """)
    List<ArticuloLista> pendientesLlamados(@Param("hogarId") UUID hogarId, @Param("nombre") String nombre);

    List<ArticuloLista> findByHogarIdAndEanAndCompradoFalse(UUID hogarId, String ean);

    long countByHogarId(UUID hogarId);

    List<ArticuloLista> findByHogarId(UUID hogarId);

    List<ArticuloLista> findByHogarIdAndCompradoFalse(UUID hogarId);

    @Modifying
    @Query("delete from ArticuloLista a where a.hogarId = :hogarId and a.comprado = true")
    int borrarComprados(@Param("hogarId") UUID hogarId);
}
