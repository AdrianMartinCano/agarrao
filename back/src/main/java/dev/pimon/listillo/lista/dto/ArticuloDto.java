package dev.pimon.listillo.lista.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code apuntadoPor} (el nombre) es null si quien lo apuntó ya borró su cuenta; el id se
 * queda para que la app le dé el mismo color de cinta que en la pantalla del hogar.
 */
public record ArticuloDto(
        UUID id,
        String nombre,
        short cantidad,
        boolean comprado,
        String apuntadoPor,
        String apuntadoPorId,
        String ean,
        LocalDateTime creadoEn,
        LocalDateTime compradoEn
) {
}
