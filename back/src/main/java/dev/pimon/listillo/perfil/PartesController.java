package dev.pimon.listillo.perfil;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;

/**
 * Qué partes de la app usa cada persona; lo apagado desaparece de su barra de abajo.
 *
 * GET /api/yo/partes   las encendidas (sin elegir nunca: todas)
 * PUT /api/yo/partes   elegirlas (al menos una)
 */
@RestController
@RequestMapping("/api/yo/partes")
@RequiredArgsConstructor
public class PartesController {

    private final PartesUsuarioRepository repositorio;

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<Set<Parte>> partes(@CurrentUser AuthUser yo) {
        return ApiResponse.ok(repositorio.findById(yo.id()).map(PartesUsuario::partes)
                .orElseGet(() -> EnumSet.allOf(Parte.class)));
    }

    @PutMapping
    @Transactional
    public ApiResponse<Set<Parte>> elegir(@CurrentUser AuthUser yo, @Valid @RequestBody PartesRequest peticion) {
        PartesUsuario partes = repositorio.findById(yo.id())
                .orElseGet(() -> new PartesUsuario(yo.id(), peticion.partes()));
        partes.elegir(peticion.partes());
        return ApiResponse.ok(repositorio.save(partes).partes());
    }
}
