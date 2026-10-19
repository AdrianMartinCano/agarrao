package dev.pimon.listillo.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Qué partes usa una persona. Se guardan las APAGADAS (V25): así una parte nueva sale
 * encendida para todos, también para quien ya había elegido. Sin fila, todas.
 */
@Entity
@Table(name = "partes_usuario")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartesUsuario {

    @Id
    @Column(name = "usuario_id", length = 36)
    private String usuarioId;

    /** Separadas por comas: «PRECIOS,GASOLINA». Vacío = todas encendidas. */
    @Column(nullable = false, length = 100)
    private String apagadas = "";

    public PartesUsuario(String usuarioId, Collection<Parte> encendidas) {
        this.usuarioId = usuarioId;
        elegir(encendidas);
    }

    public void elegir(Collection<Parte> encendidas) {
        Set<Parte> fuera = EnumSet.allOf(Parte.class);
        fuera.removeAll(encendidas);
        this.apagadas = fuera.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    /** Las encendidas. Un nombre guardado que ya no existe se ignora. */
    public Set<Parte> partes() {
        Set<Parte> encendidas = EnumSet.allOf(Parte.class);
        Arrays.stream(apagadas.split(",")).map(String::strip).filter(p -> !p.isEmpty()).forEach(p -> {
            try {
                encendidas.remove(Parte.valueOf(p));
            } catch (IllegalArgumentException e) {
                // Una parte que ya no existe.
            }
        });
        return encendidas.isEmpty() ? EnumSet.allOf(Parte.class) : encendidas;
    }
}
