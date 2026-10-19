package dev.pimon.listillo.producto;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.listillo.producto.dto.NombrarProductoRequest;
import dev.pimon.listillo.producto.dto.ProductoDto;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Piden sesión: que nadie use Listillo de proxy gratis hacia Open Food Facts.
 *
 * GET /api/productos/{ean}   qué es (404 si nadie lo conoce, 503 si OFF no responde)
 * PUT /api/productos/{ean}   ponerle nombre a uno que OFF no conoce
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService servicio;

    @GetMapping("/{ean}")
    public ApiResponse<ProductoDto> buscar(@PathVariable String ean) {
        return ApiResponse.ok(servicio.buscar(ean));
    }

    @PutMapping("/{ean}")
    public ApiResponse<ProductoDto> nombrar(@CurrentUser AuthUser yo, @PathVariable String ean,
                                            @Valid @RequestBody NombrarProductoRequest peticion) {
        return ApiResponse.ok(servicio.nombrar(ean, peticion, yo.id()));
    }
}
