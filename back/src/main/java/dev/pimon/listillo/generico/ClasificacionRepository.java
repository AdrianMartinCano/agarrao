package dev.pimon.listillo.generico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClasificacionRepository extends JpaRepository<Clasificacion, Clasificacion.Clave> {

    List<Clasificacion> findByClaveHogarId(UUID hogarId);

    /** Las líneas de los tickets del hogar desde una fecha, ya con la fecha y la cadena. */
    @Query("""
            select new dev.pimon.listillo.generico.Compra(l.id, t.id, t.fecha, t.cadena, l.texto,
                l.cantidad, l.unidad, l.importe, l.ean, l.nombre)
            from LineaTicket l join l.ticket t
            where t.hogarId = :hogarId and t.fecha >= :desde
            order by t.fecha desc, t.creadoEn desc, l.orden
            """)
    List<Compra> compras(@Param("hogarId") UUID hogarId, @Param("desde") LocalDate desde);

    /** Las líneas de un ticket del hogar. */
    @Query("""
            select new dev.pimon.listillo.generico.Compra(l.id, t.id, t.fecha, t.cadena, l.texto,
                l.cantidad, l.unidad, l.importe, l.ean, l.nombre)
            from LineaTicket l join l.ticket t
            where t.hogarId = :hogarId and t.id = :ticketId
            order by l.orden
            """)
    List<Compra> deTicket(@Param("hogarId") UUID hogarId, @Param("ticketId") UUID ticketId);
}
