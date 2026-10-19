package dev.pimon.listillo.hogar.dto;

import dev.pimon.listillo.hogar.RolMiembro;

import java.time.LocalDateTime;

public record MiembroDto(String usuarioId, String nombre, RolMiembro rol, LocalDateTime unidoEn) {
}
