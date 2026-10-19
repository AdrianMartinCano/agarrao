package dev.pimon.listillo.hogar;

import dev.pimon.listillo.hogar.dto.HogarResumenDto;
import dev.pimon.listillo.hogar.dto.MiembroDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Los listados no se paginan: un hogar tiene como mucho {@link HogarService#MAX_MIEMBROS}
 * miembros y una persona como mucho {@link HogarService#MAX_HOGARES} hogares.
 */
public interface MiembroHogarRepository extends JpaRepository<MiembroHogar, MiembroHogarId> {

    /** Los hogares de alguien, con cuántos miembros tiene cada uno, en una consulta. */
    @Query("""
            select new dev.pimon.listillo.hogar.dto.HogarResumenDto(
                h.id, h.nombre, m.rol,
                (select count(o) from MiembroHogar o where o.id.hogarId = h.id))
            from MiembroHogar m join Hogar h on h.id = m.id.hogarId
            where m.id.usuarioId = :usuarioId
            order by m.unidoEn
            """)
    List<HogarResumenDto> resumenesDe(@Param("usuarioId") String usuarioId);

    /** Los miembros con su nombre (de la tabla de usuarios de la librería), por antigüedad. */
    @Query("""
            select new dev.pimon.listillo.hogar.dto.MiembroDto(u.id, u.name, m.rol, m.unidoEn)
            from MiembroHogar m join dev.pimon.users.entity.AppUser u on u.id = m.id.usuarioId
            where m.id.hogarId = :hogarId
            order by m.unidoEn
            """)
    List<MiembroDto> miembrosDe(@Param("hogarId") UUID hogarId);

    List<MiembroHogar> findByIdHogarIdOrderByUnidoEn(UUID hogarId);

    long countByIdHogarId(UUID hogarId);

    long countByIdUsuarioId(String usuarioId);
}
