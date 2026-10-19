package dev.pimon.listillo.gasto;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pone al día los gastos fijos antes de enseñar el gasto. Va fuera de la transacción de la
 * lectura: si otra pantalla los estaba apuntando a la vez, el índice único
 * {@code uk_gastos_fijo_fecha} hace fallar a la segunda, y aquí se da por bueno (ya están).
 */
@Component
@RequiredArgsConstructor
class GastosFijosAlDia {

    private final GastoAnotadoService servicio;

    void poner(UUID hogarId, String usuarioId) {
        try {
            servicio.ponerAlDia(hogarId, usuarioId);
        } catch (DataIntegrityViolationException e) {
            // Otra petición los apuntó a la vez.
        }
    }
}
