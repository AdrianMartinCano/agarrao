package dev.pimon.listillo.hogar.dto;

import dev.pimon.listillo.hogar.RolMiembro;

import java.util.List;
import java.util.UUID;

/**
 * El hogar entero, para su pantalla: quién está, el código para invitar y los supermercados
 * que compara.
 */
public record HogarDto(
        UUID id,
        String nombre,
        String codigoInvitacion,
        RolMiembro miRol,
        List<MiembroDto> miembros,
        List<dev.pimon.listillo.ticket.Cadena> cadenas
) {
}
