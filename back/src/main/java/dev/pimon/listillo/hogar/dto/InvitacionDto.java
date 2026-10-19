package dev.pimon.listillo.hogar.dto;

/**
 * Lo que se enseña antes de entrar con un código: a qué hogar y cuánta gente hay. Sin
 * miembros ni lista: eso solo se ve ya dentro.
 */
public record InvitacionDto(String nombre, long miembros, boolean yaEstoyDentro) {
}
