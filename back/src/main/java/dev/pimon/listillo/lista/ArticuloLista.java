package dev.pimon.listillo.lista;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una línea de la lista de un hogar: texto libre («leche», «pilas AA») o, si se apuntó
 * escaneando, el nombre del producto con su código de barras.
 */
@Entity
@Table(name = "articulos_lista")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticuloLista {

    static final int MAX_CANTIDAD = 99;

    @Id
    private UUID id;

    @Column(name = "hogar_id", nullable = false, updatable = false)
    private UUID hogarId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private short cantidad;

    @Column(nullable = false)
    private boolean comprado;

    @Column(name = "creado_por", nullable = false, updatable = false, length = 36)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "comprado_por", length = 36)
    private String compradoPor;

    @Column(name = "comprado_en")
    private LocalDateTime compradoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    /** El envase escaneado, si se apuntó escaneando. */
    @Column(length = 14)
    private String ean;

    public ArticuloLista(UUID hogarId, String nombre, int cantidad, String creadoPor, String ean) {
        this.id = UUID.randomUUID();
        this.hogarId = hogarId;
        this.nombre = nombre;
        this.cantidad = (short) cantidad;
        this.creadoPor = creadoPor;
        this.creadoEn = LocalDateTime.now();
        this.ean = ean;
    }

    public void renombrar(String nombre) {
        this.nombre = nombre;
    }

    public void cambiarCantidad(int cantidad) {
        this.cantidad = (short) cantidad;
    }

    /** Volver a apuntar algo que ya está pendiente suma, sin pasar del máximo. */
    public void sumar(int mas) {
        this.cantidad = (short) Math.min(MAX_CANTIDAD, cantidad + mas);
    }

    public void marcarComprado(boolean comprado, String quien) {
        if (this.comprado == comprado) {
            return;
        }
        this.comprado = comprado;
        this.compradoPor = comprado ? quien : null;
        this.compradoEn = comprado ? LocalDateTime.now() : null;
    }

    @PrePersist
    @PreUpdate
    void marcarFecha() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
