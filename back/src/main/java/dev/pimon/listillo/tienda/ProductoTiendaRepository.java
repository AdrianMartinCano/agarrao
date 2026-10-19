package dev.pimon.listillo.tienda;

import dev.pimon.listillo.ticket.Cadena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductoTiendaRepository extends JpaRepository<ProductoTienda, ProductoTienda.Clave> {

    List<ProductoTienda> findByClaveCadena(Cadena cadena);

    List<ProductoTienda> findByGenericoIdIn(Collection<UUID> genericos);

    List<ProductoTienda> findByGenericoId(UUID generico);

    /**
     * Lo que se llama así en cualquier cadena: el nombre normalizado contiene las cuatro
     * palabras (las que falten van como «%»). Lo más barato primero; con tope.
     */
    @Query("""
            select p from ProductoTienda p
            where p.nombreNormalizado like :a and p.nombreNormalizado like :b
              and p.nombreNormalizado like :c and p.nombreNormalizado like :d
            order by p.precio
            """)
    List<ProductoTienda> buscar(@Param("a") String a, @Param("b") String b, @Param("c") String c,
                                @Param("d") String d, org.springframework.data.domain.Pageable pagina);

    /** Lo que ya no sale en esa fuente para esa cadena (descatalogado o sin precio reciente). */
    @Modifying
    @Query("""
            delete from ProductoTienda p
            where p.clave.cadena = :cadena and p.fuente = :fuente and p.actualizadoEn < :antes
            """)
    int borrarNoVistosDe(@Param("cadena") Cadena cadena, @Param("fuente") dev.pimon.listillo.generico.FuentePrecio fuente,
                         @Param("antes") LocalDateTime antes);

    /** Todos los nombres normalizados, para contar cuánto dice cada palabra. */
    @Query("select p.nombreNormalizado from ProductoTienda p")
    List<String> nombresNormalizados();

    /** Cuándo se importó por última vez de esa fuente. */
    @Query("select max(p.actualizadoEn) from ProductoTienda p where p.fuente = :fuente")
    java.util.Optional<LocalDateTime> ultimaImportacion(@Param("fuente") dev.pimon.listillo.generico.FuentePrecio fuente);

    /** Lo que ya no sale en la web de esa cadena (descatalogado). */
    @Modifying
    @Query("delete from ProductoTienda p where p.clave.cadena = :cadena and p.actualizadoEn < :antes")
    int borrarNoVistos(@Param("cadena") Cadena cadena, @Param("antes") LocalDateTime antes);
}
