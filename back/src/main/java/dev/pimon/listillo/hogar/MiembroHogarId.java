package dev.pimon.listillo.hogar;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiembroHogarId implements Serializable {

    @Column(name = "hogar_id", nullable = false)
    private UUID hogarId;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    public MiembroHogarId(UUID hogarId, String usuarioId) {
        this.hogarId = hogarId;
        this.usuarioId = usuarioId;
    }
}
