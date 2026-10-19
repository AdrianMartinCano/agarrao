package dev.pimon.listillo.hogar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Un grupo de personas que comparten una lista de la compra. */
@Entity
@Table(name = "hogares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hogar {

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(name = "codigo_invitacion", nullable = false, length = 8)
    private String codigoInvitacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** Las cadenas que compara la casa, separadas por comas. Null = todas. */
    @Column(length = 300)
    private String cadenas;

    public Hogar(String nombre, String codigoInvitacion) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.codigoInvitacion = codigoInvitacion;
        this.creadoEn = LocalDateTime.now();
    }

    public void renombrar(String nombre) {
        this.nombre = nombre;
    }

    /** Las cadenas que la casa ve y compara (todas si no ha elegido). */
    public java.util.Set<dev.pimon.listillo.ticket.Cadena> cadenasVisibles() {
        java.util.Set<dev.pimon.listillo.ticket.Cadena> todas =
                java.util.EnumSet.allOf(dev.pimon.listillo.ticket.Cadena.class);
        if (cadenas == null || cadenas.isBlank()) {
            return todas;
        }
        java.util.Set<dev.pimon.listillo.ticket.Cadena> elegidas =
                java.util.EnumSet.noneOf(dev.pimon.listillo.ticket.Cadena.class);
        for (String c : cadenas.split(",")) {
            try {
                elegidas.add(dev.pimon.listillo.ticket.Cadena.valueOf(c.strip()));
            } catch (IllegalArgumentException e) {
                // Una cadena que ya no existe se ignora.
            }
        }
        // «Otra tienda» siempre: son tickets de sitios sin catálogo.
        elegidas.add(dev.pimon.listillo.ticket.Cadena.OTRA);
        return elegidas.size() > 1 ? elegidas : todas;
    }

    /** Elegir todas vuelve a «todas» (null): así una cadena nueva sale sin tener que marcarla. */
    public void elegirCadenas(java.util.Set<dev.pimon.listillo.ticket.Cadena> elegidas) {
        java.util.Set<dev.pimon.listillo.ticket.Cadena> sinOtra = java.util.EnumSet.copyOf(elegidas);
        sinOtra.remove(dev.pimon.listillo.ticket.Cadena.OTRA);
        boolean todas = sinOtra.size() >= dev.pimon.listillo.ticket.Cadena.values().length - 1;
        this.cadenas = todas ? null : sinOtra.stream().map(Enum::name).sorted()
                .collect(java.util.stream.Collectors.joining(","));
    }

    /** El código viejo deja de valer: sirve si se ha compartido donde no debía. */
    public void cambiarCodigo(String codigo) {
        this.codigoInvitacion = codigo;
    }
}
