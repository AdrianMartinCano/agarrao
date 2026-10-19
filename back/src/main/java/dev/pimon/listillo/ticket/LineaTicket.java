package dev.pimon.listillo.ticket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Una línea del ticket con lo que se pagó. Casada con un producto (ean y nombre), solo con
 * un nombre (algo de la lista sin código), o sin casar (los dos a null).
 */
@Entity
@Table(name = "lineas_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LineaTicket {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Column(nullable = false)
    private short orden;

    @Column(nullable = false, length = 120)
    private String texto;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Unidad unidad;

    @Column(name = "precio_unitario", precision = 10, scale = 3)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(length = 14)
    private String ean;

    @Column(length = 120)
    private String nombre;

    public LineaTicket(String texto, BigDecimal cantidad, Unidad unidad, BigDecimal precioUnitario,
                       BigDecimal importe, String ean, String nombre) {
        this.id = UUID.randomUUID();
        this.texto = texto;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.precioUnitario = precioUnitario;
        this.importe = importe;
        this.ean = ean;
        this.nombre = nombre;
    }

    void asignar(Ticket ticket, int orden) {
        this.ticket = ticket;
        this.orden = (short) orden;
    }

    public boolean casada() {
        return nombre != null;
    }
}
