package dev.pimon.listillo.gasolina.repostajes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    /** Sin paginar: son los coches de una persona, no un catálogo. */
    List<Vehiculo> findByHogarIdAndActivoTrueOrderByCreatedAtAsc(java.util.UUID hogarId);

    /**
     * Búsqueda por id Y usuario, siempre juntos.
     *
     * Es lo que impide que alguien registre un repostaje en el coche de otro pasando un
     * id ajeno: si el vehículo no es suyo, aquí no aparece.
     */
    Optional<Vehiculo> findByIdAndHogarId(String id, java.util.UUID hogarId);

    long countByHogarIdAndActivoTrue(java.util.UUID hogarId);
}
