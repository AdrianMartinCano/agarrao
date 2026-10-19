package dev.pimon.listillo.generico;

import dev.pimon.listillo.ticket.Unidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se compra sin mirar marca ni tienda («Leche semidesnatada»), comparado por precio
 * por unidad. Del catálogo de Listillo (sin hogar) o creado por un hogar.
 */
@Entity
@Table(name = "genericos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Generico {

    @Id
    private UUID id;

    /** Null = del catálogo, para todos los hogares. */
    @Column(name = "hogar_id", updatable = false)
    private UUID hogarId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Unidad unidad;

    @Column(length = 400)
    private String pistas;

    @Column(length = 40)
    private String retrato;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Generico(UUID hogarId, String nombre, Categoria categoria, Unidad unidad) {
        this.id = UUID.randomUUID();
        this.hogarId = hogarId;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidad = unidad;
        this.creadoEn = LocalDateTime.now();
    }

    public boolean propio() {
        return hogarId != null;
    }

    /** Las pistas sin las categorías de Open Food Facts: otras formas de llamarlo. */
    List<String> otrosNombres() {
        return trozosDePistas().filter(p -> !p.startsWith("en:") && !p.startsWith("-")).toList();
    }

    /**
     * Palabras que dicen que NO es este genérico (pistas con «-»: «-codorniz» en Huevos,
     * «-carcasa» en Pollo). Normalizadas.
     */
    List<String> excluidas() {
        return trozosDePistas().filter(p -> p.startsWith("-"))
                .map(p -> dev.pimon.listillo.ticket.Textos.normalizar(p.substring(1)))
                .filter(p -> !p.isEmpty())
                .toList();
    }

    /** Las categorías de Open Food Facts («en:eggs») que son este genérico. */
    List<String> categoriasOff() {
        return trozosDePistas().filter(p -> p.startsWith("en:")).toList();
    }

    private java.util.stream.Stream<String> trozosDePistas() {
        return pistas == null ? java.util.stream.Stream.empty()
                : Arrays.stream(pistas.split(",")).map(String::strip).filter(p -> !p.isEmpty());
    }
}
