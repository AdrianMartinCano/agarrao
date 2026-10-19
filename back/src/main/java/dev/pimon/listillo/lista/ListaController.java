package dev.pimon.listillo.lista;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.lista.dto.ArticuloDto;
import dev.pimon.listillo.lista.dto.CambiarArticuloRequest;
import dev.pimon.listillo.lista.dto.NuevoArticuloRequest;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Todas devuelven la lista entera y ordenada: el móvil la sustituye sin más y de paso ve
 * lo que han cambiado los demás.
 *
 * GET    /api/hogares/{h}/lista
 * POST   /api/hogares/{h}/lista               apuntar (suma si ya está pendiente)
 * PATCH  /api/hogares/{h}/lista/{articulo}    nombre, cantidad o comprado
 * DELETE /api/hogares/{h}/lista/{articulo}
 * DELETE /api/hogares/{h}/lista/comprados     vaciar lo ya comprado
 */
@RestController
@RequestMapping("/api/hogares/{hogarId}/lista")
@RequiredArgsConstructor
public class ListaController {

    private final ListaService servicio;

    @GetMapping
    public ApiResponse<List<ArticuloDto>> lista(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.lista(hogarId, yo.id()));
    }

    @PostMapping
    public ApiResponse<List<ArticuloDto>> apuntar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                  @Valid @RequestBody NuevoArticuloRequest peticion) {
        return ApiResponse.ok(servicio.apuntar(hogarId, yo.id(), peticion));
    }

    @PatchMapping("/{articuloId}")
    public ApiResponse<List<ArticuloDto>> cambiar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                  @PathVariable UUID articuloId,
                                                  @Valid @RequestBody CambiarArticuloRequest peticion) {
        return ApiResponse.ok(servicio.cambiar(hogarId, articuloId, yo.id(), peticion));
    }

    @DeleteMapping("/comprados")
    public ApiResponse<List<ArticuloDto>> quitarComprados(@CurrentUser AuthUser yo, @PathVariable UUID hogarId) {
        return ApiResponse.ok(servicio.quitarComprados(hogarId, yo.id()));
    }

    @DeleteMapping("/{articuloId}")
    public ApiResponse<List<ArticuloDto>> quitar(@CurrentUser AuthUser yo, @PathVariable UUID hogarId,
                                                 @PathVariable UUID articuloId) {
        return ApiResponse.ok(servicio.quitar(hogarId, articuloId, yo.id()));
    }
}
