-- ---------------------------------------------------------------------------
-- V24 · Gasolina, traída de Gasofa.
--
-- Mismas columnas que en Gasofa (gaspath V1–V6) para poder pasar sus datos tal cual, con
-- un cambio de fondo: los vehículos y los repostajes son del HOGAR (hogar_id) y no de una
-- persona. Un coche de casa lo ven todos y cada repostaje cuenta una sola vez en Gasto.
-- Las gasolineras no se guardan: el catálogo del Ministerio vive en memoria
-- (CatalogoEstaciones) y se refresca cada 30 minutos.
-- ---------------------------------------------------------------------------

-- Las favoritas sí son de cada persona (como en Gasofa).
CREATE TABLE favoritos (
    id          varchar(36)     NOT NULL,
    created_at  timestamp(6)    NOT NULL,
    updated_at  timestamp(6)    NOT NULL,
    user_id     varchar(36)     NOT NULL,
    estacion_id varchar(32)     NOT NULL,
    CONSTRAINT favoritos_pkey PRIMARY KEY (id),
    CONSTRAINT uk_favoritos_usuario_estacion UNIQUE (user_id, estacion_id),
    CONSTRAINT fk_favoritos_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_favoritos_user_created ON favoritos (user_id, created_at DESC);

CREATE TABLE vehiculos (
    id          varchar(36)     NOT NULL,
    created_at  timestamp(6)    NOT NULL,
    updated_at  timestamp(6)    NOT NULL,
    hogar_id    uuid            NOT NULL,
    nombre      varchar(60)     NOT NULL,
    carburante  varchar(40)     NOT NULL,
    matricula   varchar(15),
    -- Se archivan, no se borran: un coche vendido conserva su historial.
    activo      boolean         NOT NULL DEFAULT true,
    CONSTRAINT vehiculos_pkey PRIMARY KEY (id),
    CONSTRAINT fk_vehiculos_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE
);

CREATE INDEX idx_vehiculos_hogar ON vehiculos (hogar_id, activo);

CREATE TABLE repostajes (
    id                      varchar(36)     NOT NULL,
    created_at              timestamp(6)    NOT NULL,
    updated_at              timestamp(6)    NOT NULL,
    vehiculo_id             varchar(36)     NOT NULL,
    hogar_id                uuid            NOT NULL,
    -- Quién lo apuntó. Sin FK: si borra su cuenta, el repostaje sigue siendo de la casa.
    creado_por              varchar(36),
    fecha                   date            NOT NULL,
    -- La gasolinera, CONGELADA: si cierra, desaparece del catálogo y el historial no.
    estacion_id             varchar(32),
    estacion_nombre         varchar(120),
    estacion_municipio      varchar(120),
    carburante              varchar(40)     NOT NULL,
    litros                  numeric(8,3)    NOT NULL,
    precio_litro            numeric(6,3)    NOT NULL,
    importe                 numeric(8,2)    NOT NULL,
    kilometros              integer,
    -- El consumo solo se mide entre dos llenados completos.
    deposito_lleno          boolean         NOT NULL DEFAULT true,
    precio_medio_provincia  numeric(6,3),
    notas                   varchar(300),
    precio_medio_zona       numeric(6,3),
    precio_minimo_zona      numeric(6,3),
    radio_zona_km           integer,
    CONSTRAINT repostajes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_repostajes_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id) ON DELETE CASCADE,
    CONSTRAINT fk_repostajes_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE,
    CONSTRAINT repostajes_litros_check CHECK (litros > 0),
    CONSTRAINT repostajes_precio_check CHECK (precio_litro > 0),
    CONSTRAINT repostajes_km_check     CHECK (kilometros IS NULL OR kilometros >= 0)
);

CREATE INDEX idx_repostajes_vehiculo_fecha ON repostajes (vehiculo_id, fecha);
CREATE INDEX idx_repostajes_hogar_fecha ON repostajes (hogar_id, fecha DESC);
