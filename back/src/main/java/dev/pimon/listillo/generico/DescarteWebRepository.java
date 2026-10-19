package dev.pimon.listillo.generico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DescarteWebRepository extends JpaRepository<DescarteWeb, DescarteWeb.Clave> {

    List<DescarteWeb> findByClaveGenericoIdIn(Collection<UUID> genericos);
}
