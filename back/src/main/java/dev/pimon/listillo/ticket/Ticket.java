package dev.pimon.listillo.ticket;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Un ticket de compra de un hogar, con sus líneas. */
@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    private UUID id;

    @Column(name = "hogar_id", nullable = false, updatable = false)
    private UUID hogarId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cadena cadena;

    @Column(name = "tienda_texto", length = 80)
    private String tiendaTexto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OrigenTicket origen;

    @Column(name = "subido_por", nullable = false, length = 36)
    private String subidoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<LineaTicket> lineas = new ArrayList<>();

    public Ticket(UUID hogarId, Cadena cadena, String tiendaTexto, LocalDate fecha, BigDecimal total,
                  OrigenTicket origen, String subidoPor) {
        this.id = UUID.randomUUID();
        this.hogarId = hogarId;
        this.cadena = cadena;
        this.tiendaTexto = tiendaTexto;
        this.fecha = fecha;
        this.total = total;
        this.origen = origen;
        this.subidoPor = subidoPor;
        this.creadoEn = LocalDateTime.now();
    }

    public void anadir(LineaTicket linea) {
        linea.asignar(this, lineas.size());
        lineas.add(linea);
    }
}
