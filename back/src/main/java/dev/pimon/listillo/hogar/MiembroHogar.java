package dev.pimon.listillo.hogar;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Que alguien pertenece a un hogar. Se guardan los ids y no relaciones JPA: el usuario es
 * una entidad de la librería spring-users y así este módulo no depende de su forma.
 */
@Entity
@Table(name = "miembros_hogar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiembroHogar {

    @EmbeddedId
    private MiembroHogarId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RolMiembro rol;

    @Column(name = "unido_en", nullable = false, updatable = false)
    private LocalDateTime unidoEn;

    public MiembroHogar(UUID hogarId, String usuarioId, RolMiembro rol) {
        this.id = new MiembroHogarId(hogarId, usuarioId);
        this.rol = rol;
        this.unidoEn = LocalDateTime.now();
    }

    public boolean esPropietario() {
        return rol == RolMiembro.PROPIETARIO;
    }

    public void hacerPropietario() {
        this.rol = RolMiembro.PROPIETARIO;
    }

    public String getUsuarioId() {
        return id.getUsuarioId();
    }
}
