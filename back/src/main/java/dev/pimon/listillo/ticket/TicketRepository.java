package dev.pimon.listillo.ticket;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /** Los últimos tickets del hogar, sin las líneas (para el listado). */
    List<Ticket> findByHogarIdOrderByFechaDescCreadoEnDesc(UUID hogarId, Pageable pagina);

    /** Los tickets del hogar entre dos fechas (incluidas), sin las líneas: para los gráficos. */
    List<Ticket> findByHogarIdAndFechaBetween(UUID hogarId, LocalDate desde, LocalDate hasta);

    /** Un ticket con sus líneas en una sola consulta. */
    @EntityGraph(attributePaths = "lineas")
    Optional<Ticket> findWithLineasByIdAndHogarId(UUID id, UUID hogarId);

    /** Conteo de líneas y casadas por ticket, para el listado sin cargar las líneas. */
    @Query("""
            select new dev.pimon.listillo.ticket.ConteoLineas(l.ticket.id, count(l), count(l.nombre))
            from LineaTicket l where l.ticket.id in :ids group by l.ticket.id
            """)
    List<ConteoLineas> contarLineas(@Param("ids") List<UUID> ids);

    /** Con qué se han casado antes líneas de este hogar: candidatos para casar las nuevas. */
    @Query("""
            select distinct new dev.pimon.listillo.ticket.Candidato(l.ean, l.nombre)
            from LineaTicket l where l.ticket.hogarId = :hogarId and l.nombre is not null
            """)
    List<Candidato> casadasDelHogar(@Param("hogarId") UUID hogarId);
}
