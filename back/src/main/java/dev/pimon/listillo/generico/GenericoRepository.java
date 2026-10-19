package dev.pimon.listillo.generico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenericoRepository extends JpaRepository<Generico, UUID> {

    /** El catálogo de Listillo más los genéricos propios del hogar. */
    @Query("select g from Generico g where g.hogarId is null or g.hogarId = :hogarId order by g.nombre")
    List<Generico> delHogar(@Param("hogarId") UUID hogarId);

    /** Uno que el hogar puede ver: del catálogo o suyo. */
    @Query("select g from Generico g where g.id = :id and (g.hogarId is null or g.hogarId = :hogarId)")
    Optional<Generico> visible(@Param("id") UUID id, @Param("hogarId") UUID hogarId);

    @Query("""
            select count(g) > 0 from Generico g
            where lower(g.nombre) = lower(:nombre) and (g.hogarId is null or g.hogarId = :hogarId)
            """)
    boolean existeNombre(@Param("hogarId") UUID hogarId, @Param("nombre") String nombre);

    long countByHogarId(UUID hogarId);

    /** El catálogo de Listillo, sin los propios de ningún hogar. */
    List<Generico> findByHogarIdIsNull();
}
