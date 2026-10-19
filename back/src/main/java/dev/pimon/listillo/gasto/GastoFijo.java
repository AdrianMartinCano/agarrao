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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se repite cada mes (Netflix, Spotify): cada mes, al llegar su día, se apunta su
 * gasto. Se pone al día al mirar los gastos, así que no se pierde ningún mes aunque el
 * servidor estuviera parado.
 */
@Entity
@Table(name = "gastos_fijos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GastoFijo {

    /** Hasta el 28: así el día existe en todos los meses. */
    static final int ULTIMO_DIA = 28;

    @Id
    private UUID id;

    @Column(name = "hogar_id", nullable = false, updatable = false)
    private UUID hogarId;

    @Column(nullable = false, length = 80)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoGasto tipo;

    @Column(nullable = false)
    private short dia;

    /** El último mes que ya tiene su gasto (su día 1). */
    @Column(name = "ultimo_mes", nullable = false)
    private LocalDate ultimoMes;

    @Column(name = "creado_por", nullable = false, updatable = false, length = 36)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** Empieza con el gasto de {@code primero} ya apuntado. */
    public GastoFijo(UUID hogarId, String concepto, BigDecimal importe, TipoGasto tipo, LocalDate primero,
                     String creadoPor) {
        this.id = UUID.randomUUID();
        this.hogarId = hogarId;
        this.concepto = concepto;
        this.importe = importe;
        this.tipo = tipo;
        this.dia = (short) Math.min(ULTIMO_DIA, primero.getDayOfMonth());
        this.ultimoMes = primero.withDayOfMonth(1);
        this.creadoPor = creadoPor;
        this.creadoEn = LocalDateTime.now();
    }

    /** Los gastos de los meses que ya han llegado a su día y aún no están apuntados. */
    public List<Gasto> pendientes(LocalDate hoy) {
        List<Gasto> nuevos = new ArrayList<>();
        YearMonth mes = YearMonth.from(ultimoMes).plusMonths(1);
        while (!mes.atDay(dia).isAfter(hoy)) {
            nuevos.add(new Gasto(hogarId, concepto, importe, mes.atDay(dia), tipo, id, creadoPor));
            ultimoMes = mes.atDay(1);
            mes = mes.plusMonths(1);
        }
        return nuevos;
    }
}
