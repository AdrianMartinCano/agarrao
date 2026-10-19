package dev.pimon.listillo.gasto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GastoRepository extends JpaRepository<Gasto, UUID> {

    List<Gasto> findByHogarIdAndFechaBetweenOrderByFechaDescCreadoEnDesc(UUID hogarId, LocalDate desde, LocalDate hasta);

    Optional<Gasto> findByIdAndHogarId(UUID id, UUID hogarId);
}
