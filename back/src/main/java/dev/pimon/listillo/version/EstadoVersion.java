package dev.pimon.listillo.version;

/** Qué tiene que hacer la app instalada con su versión. */
public enum EstadoVersion {
    /** Está al día. */
    AL_DIA,
    /** Hay una más nueva, pero la instalada sigue funcionando: aviso que se puede cerrar. */
    OPCIONAL,
    /** La instalada ya no vale con este servidor: pantalla que bloquea hasta actualizar. */
    OBLIGATORIA
}
