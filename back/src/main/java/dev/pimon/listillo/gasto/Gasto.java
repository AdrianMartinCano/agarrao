package dev.pimon.listillo.gasto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Un gasto anotado (no un ticket del súper): la cena del sábado, Netflix de octubre… */
@Entity
@Table(name = "gastos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gasto {

    @Id
    private UUID id;

    @Column(name = "hogar_id", nullable = false, updatable = false)
    private UUID hogarId;

    @Column(nullable = false, length = 80)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoGasto tipo;

    /** De qué gasto fijo salió, si salió de uno. */
    @Column(name = "fijo_id", updatable = false)
    private UUID fijoId;

    @Column(name = "creado_por", nullable = false, updatable = false, length = 36)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Gasto(UUID hogarId, String concepto, BigDecimal importe, LocalDate fecha, TipoGasto tipo, UUID fijoId,
                 String creadoPor) {
        this.id = UUID.randomUUID();
        this.hogarId = hogarId;
        this.concepto = concepto;
        this.importe = importe;
        this.fecha = fecha;
        this.tipo = tipo;
        this.fijoId = fijoId;
        this.creadoPor = creadoPor;
        this.creadoEn = LocalDateTime.now();
    }

    public void cambiar(String concepto, BigDecimal importe, LocalDate fecha, TipoGasto tipo) {
        this.concepto = concepto;
        this.importe = importe;
        this.fecha = fecha;
        this.tipo = tipo;
    }
}
