package dev.pimon.listillo.tienda;

import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.exception.AppException;
import dev.pimon.listillo.ticket.Cadena;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * POST /api/admin/precios-web/importar[?cadena=LIDL]   importa ya los catálogos web, todos o
 * uno (ROLE_ADMIN; tarda minutos).
 * POST /api/admin/open-prices/importar                 importa ya los precios de Open Prices.
 */
@RestController
@RequiredArgsConstructor
public class AdminPreciosController {

    private final ImportadorPrecios importador;
    private final PreciosWebProperties props;
    private final ImportadorOpenPrices openPrices;
    private final OpenPricesProperties openPricesProps;

    @PostMapping("/api/admin/open-prices/importar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<Cadena, Integer>> importarOpenPrices() {
        if (!openPricesProps.activo()) {
            throw AppException.conflict("Open Prices está apagado (OPEN_PRICES)");
        }
        return ApiResponse.ok(openPrices.importar());
    }

    @PostMapping("/api/admin/precios-web/importar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<Cadena, Integer>> importar(@RequestParam(required = false) Cadena cadena) {
        if (!props.activo()) {
            throw AppException.conflict("Leer precios de las webs está apagado (PRECIOS_WEB)");
        }
        return ApiResponse.ok(cadena == null ? importador.importarTodo() : importador.importar(cadena));
    }
}
