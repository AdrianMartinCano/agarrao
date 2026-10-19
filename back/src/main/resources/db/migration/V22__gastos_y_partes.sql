-- ---------------------------------------------------------------------------
-- V22 · Gastos que no son del súper y las partes que usa cada persona.
--
-- Lo del súper sigue en `tickets` (con sus líneas, que alimentan Precios). Aquí va todo lo
-- demás: la cena del sábado, Netflix, la gasolina… anotado a mano o leído de un ticket.
-- No se reparte entre los de casa: solo se apunta para verlo en las gráficas.
-- ---------------------------------------------------------------------------

-- Lo que se repite cada mes (Netflix, Spotify, el gimnasio): se apunta una vez y cada mes,
-- al llegar su día, aparece su gasto.
CREATE TABLE gastos_fijos (
    id              uuid            NOT NULL,
    hogar_id        uuid            NOT NULL,
    concepto        varchar(80)     NOT NULL,
    importe         numeric(10, 2)  NOT NULL CHECK (importe > 0),
    tipo            varchar(12)     NOT NULL,
    -- Del 1 al 28: así existe en todos los meses.
    dia             smallint        NOT NULL CHECK (dia BETWEEN 1 AND 28),
    -- El último mes que ya tiene su gasto («2026-10-01»): lo siguiente que toca es el de después.
    ultimo_mes      date            NOT NULL,
    creado_por      varchar(36)     NOT NULL,
    creado_en       timestamp(6)    NOT NULL,

    CONSTRAINT gastos_fijos_pkey PRIMARY KEY (id),
    CONSTRAINT fk_gastos_fijos_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE
);

CREATE INDEX idx_gastos_fijos_hogar ON gastos_fijos (hogar_id);

CREATE TABLE gastos (
    id              uuid            NOT NULL,
    hogar_id        uuid            NOT NULL,
    concepto        varchar(80)     NOT NULL,
    importe         numeric(10, 2)  NOT NULL CHECK (importe > 0),
    fecha           date            NOT NULL,
    tipo            varchar(12)     NOT NULL,
    -- De qué gasto fijo salió. Quitar el fijo no borra los meses ya apuntados.
    fijo_id         uuid,
    -- Sin FK, como en la lista: si esa persona borra su cuenta, el gasto se queda.
    creado_por      varchar(36)     NOT NULL,
    creado_en       timestamp(6)    NOT NULL,

    CONSTRAINT gastos_pkey PRIMARY KEY (id),
    CONSTRAINT fk_gastos_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE,
    CONSTRAINT fk_gastos_fijo FOREIGN KEY (fijo_id) REFERENCES gastos_fijos (id) ON DELETE SET NULL
);

CREATE INDEX idx_gastos_hogar_fecha ON gastos (hogar_id, fecha DESC);
-- Un mes de un gasto fijo, una sola vez: si dos pantallas lo ponen al día a la vez, la
-- segunda choca aquí y no se apunta dos veces.
CREATE UNIQUE INDEX uk_gastos_fijo_fecha ON gastos (fijo_id, fecha) WHERE fijo_id IS NOT NULL;

-- Qué partes de la app usa cada persona («LISTA,PRECIOS,GASTOS»). Sin fila = todas.
CREATE TABLE partes_usuario (
    usuario_id      varchar(36)     NOT NULL,
    partes          varchar(100)    NOT NULL,

    CONSTRAINT partes_usuario_pkey PRIMARY KEY (usuario_id),
    CONSTRAINT fk_partes_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES app_users (id) ON DELETE CASCADE
);
