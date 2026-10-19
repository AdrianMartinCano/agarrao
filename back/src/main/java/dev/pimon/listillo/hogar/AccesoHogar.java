package dev.pimon.listillo.hogar;

import dev.pimon.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * La única puerta a los datos de un hogar: quien no es miembro recibe un 404, no un 403,
 * para no confirmar que ese hogar existe. La usan el hogar y la lista.
 */
@Component
@RequiredArgsConstructor
public class AccesoHogar {

    private final MiembroHogarRepository miembros;

    @Transactional(readOnly = true)
    public MiembroHogar exigirMiembro(UUID hogarId, String usuarioId) {
        return miembros.findById(new MiembroHogarId(hogarId, usuarioId))
                .orElseThrow(() -> AppException.notFound("Ese hogar no existe o no eres de él"));
    }
}
