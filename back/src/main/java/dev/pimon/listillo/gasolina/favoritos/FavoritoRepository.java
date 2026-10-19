package dev.pimon.listillo.gasolina.favoritos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, String> {

    /**
     * Sin paginar a propósito: son los favoritos de UN usuario, un puñado de filas por
     * definición. Paginar aquí complicaría el front sin resolver ningún problema real.
     */
    List<Favorito> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Favorito> findByUserIdAndEstacionId(String userId, String estacionId);

    boolean existsByUserIdAndEstacionId(String userId, String estacionId);
}
