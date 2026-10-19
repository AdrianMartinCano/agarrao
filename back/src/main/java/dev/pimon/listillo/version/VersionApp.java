package dev.pimon.listillo.version;

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

/**
 * La versión publicada de la app. Hay UNA sola fila (id = 1): Listillo es una PWA, así
 * que no hay tiendas ni una versión por plataforma; Android, iPhone y escritorio cargan
 * la misma web.
 *
 * Vive en la base de datos y no en variables de entorno para poder
 * forzar una actualización desde el panel sin redesplegar el back: si una versión sale
 * con un fallo grave, subir la mínima es cosa de un minuto.
 */
@Entity
@Table(name = "version_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VersionApp {

    /** La única fila. */
    public static final short ID = 1;

    @Id
    private Short id;

    /** La última publicada. Si la instalada es menor, aviso que se puede cerrar. */
    @Column(nullable = false, length = 20)
    private String ultima;

    /** Por debajo de esta, la app no deja seguir hasta actualizar. */
    @Column(nullable = false, length = 20)
    private String minima;

    /** Novedades que se enseñan en el aviso. */
    @Column(length = 500)
    private String notas;

    @Column(name = "actualizada_en", nullable = false)
    private LocalDateTime actualizadaEn;

    public void actualizar(String ultima, String minima, String notas) {
        this.ultima = ultima;
        this.minima = minima;
        this.notas = notas;
    }

    @PrePersist
    @PreUpdate
    void marcarFecha() {
        this.actualizadaEn = LocalDateTime.now();
    }
}
