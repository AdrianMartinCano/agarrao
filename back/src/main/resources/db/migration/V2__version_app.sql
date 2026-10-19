-- ---------------------------------------------------------------------------
-- V2 · La versión publicada de la app.
--
-- Una sola fila (id = 1): Listillo es una PWA, todos los dispositivos cargan la misma
-- web, así que no hay una versión por plataforma. Va en la base y no en variables de
-- entorno para poder forzar una actualización desde el panel sin redesplegar.
-- ---------------------------------------------------------------------------
CREATE TABLE version_app (
    id              smallint        NOT NULL,
    ultima          varchar(20)     NOT NULL,
    minima          varchar(20)     NOT NULL,
    notas           varchar(500),
    actualizada_en  timestamp(6)    NOT NULL,

    CONSTRAINT version_app_pkey PRIMARY KEY (id),
    -- Que nadie meta una segunda fila: el código solo lee la 1.
    CONSTRAINT ck_version_app_unica CHECK (id = 1)
);

INSERT INTO version_app (id, ultima, minima, notas, actualizada_en)
VALUES (1, '0.1.0', '0.1.0', NULL, now());
