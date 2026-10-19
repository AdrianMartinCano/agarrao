package dev.pimon.listillo.hogar;

import dev.pimon.listillo.ticket.Cadena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Qué supermercados ve y compara un hogar (Ajustes). Lo usan Precios, el buscador y el reparto. */
@Component
@RequiredArgsConstructor
public class CadenasDelHogar {

    private final HogarRepository hogares;

    @Transactional(readOnly = true)
    public Set<Cadena> de(UUID hogarId) {
        return hogares.findById(hogarId).map(Hogar::cadenasVisibles).orElse(EnumSet.allOf(Cadena.class));
    }
}
