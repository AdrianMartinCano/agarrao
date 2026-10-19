package dev.pimon.listillo.gasolina.repostajes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepostajeRepository extends JpaRepository<Repostaje, String> {

    /**
     * Historial completo de un vehículo, en orden cronológico.
     *
     * Sin paginar a propósito, y es una excepción consciente a la regla de paginar los
     * listados: el cálculo de consumo necesita TODO el historial para encadenar los
     * tramos entre llenados. Repostando tres veces al mes son unas 36 filas al año, así
     * que el coste es despreciable; si algún día alguien acumula miles, habrá que acotar
     * por fechas.
     */
    List<Repostaje> findByVehiculoIdOrderByFechaAsc(String vehiculoId);

    /** Últimos repostajes del hogar, para la portada del historial. */
    /** Los del hogar entre dos fechas: para sumarlos en Gasto como gasolina. */
    List<Repostaje> findByHogarIdAndFechaBetween(java.util.UUID hogarId, java.time.LocalDate desde,
                                                 java.time.LocalDate hasta);

    List<Repostaje> findByHogarIdOrderByFechaDesc(java.util.UUID hogarId);

    /** Por id y hogar juntos: nadie puede tocar el repostaje de otro. */
    Optional<Repostaje> findByIdAndHogarId(String id, java.util.UUID hogarId);
}
