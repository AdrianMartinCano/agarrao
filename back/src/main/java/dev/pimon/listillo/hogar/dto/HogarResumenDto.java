package dev.pimon.listillo.hogar.dto;

import dev.pimon.listillo.hogar.RolMiembro;

import java.util.UUID;

/** Un hogar en «mis hogares»: lo justo para el selector. */
public record HogarResumenDto(UUID id, String nombre, RolMiembro miRol, long miembros) {
}
