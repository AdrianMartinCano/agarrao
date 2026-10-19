package dev.pimon.listillo.hogar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HogarRepository extends JpaRepository<Hogar, UUID> {

    Optional<Hogar> findByCodigoInvitacion(String codigo);

    boolean existsByCodigoInvitacion(String codigo);
}
