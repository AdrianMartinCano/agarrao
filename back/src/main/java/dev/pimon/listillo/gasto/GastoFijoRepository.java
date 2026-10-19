package dev.pimon.listillo.gasto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GastoFijoRepository extends JpaRepository<GastoFijo, UUID> {

    List<GastoFijo> findByHogarIdOrderByConceptoAsc(UUID hogarId);

    Optional<GastoFijo> findByIdAndHogarId(UUID id, UUID hogarId);
}
