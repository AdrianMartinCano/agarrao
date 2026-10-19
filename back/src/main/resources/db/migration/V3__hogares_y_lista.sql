-- ---------------------------------------------------------------------------
-- V3 · Hogares y lista compartida.
--
-- Un hogar es un grupo de personas que comparten UNA lista de la compra. Se entra con un
-- código de invitación de 8 caracteres. Una persona puede estar en varios hogares (su
-- casa y la de sus padres, por ejemplo).
-- ---------------------------------------------------------------------------

CREATE TABLE hogares (
    id                  uuid            NOT NULL,
    nombre              varchar(40)     NOT NULL,
    -- Sin caracteres que se confundan al dictarlo (0/O, 1/I/L). Se puede regenerar.
    codigo_invitacion   varchar(8)      NOT NULL,
    creado_en           timestamp(6)    NOT NULL,

    CONSTRAINT hogares_pkey PRIMARY KEY (id),
    CONSTRAINT hogares_codigo_uk UNIQUE (codigo_invitacion)
);

CREATE TABLE miembros_hogar (
    hogar_id    uuid            NOT NULL,
    usuario_id  varchar(36)     NOT NULL,
    rol         varchar(12)     NOT NULL CHECK (rol IN ('PROPIETARIO', 'MIEMBRO')),
    unido_en    timestamp(6)    NOT NULL,

    CONSTRAINT miembros_hogar_pkey PRIMARY KEY (hogar_id, usuario_id),
    CONSTRAINT fk_miembros_hogar_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE,
    -- Borrar la cuenta saca a la persona de sus hogares.
    CONSTRAINT fk_miembros_hogar_usuario FOREIGN KEY (usuario_id) REFERENCES app_users (id) ON DELETE CASCADE
);

-- «Mis hogares»: se busca por usuario, y la PK empieza por hogar.
CREATE INDEX idx_miembros_hogar_usuario ON miembros_hogar (usuario_id);

CREATE TABLE articulos_lista (
    id              uuid            NOT NULL,
    hogar_id        uuid            NOT NULL,
    nombre          varchar(80)     NOT NULL,
    cantidad        smallint        NOT NULL DEFAULT 1 CHECK (cantidad BETWEEN 1 AND 99),
    comprado        boolean         NOT NULL DEFAULT false,
    -- Quién lo apuntó y quién lo echó al carro. Sin FK: si esa persona borra su cuenta, el
    -- artículo sigue en la lista de los demás.
    creado_por      varchar(36)     NOT NULL,
    creado_en       timestamp(6)    NOT NULL,
    comprado_por    varchar(36),
    comprado_en     timestamp(6),
    actualizado_en  timestamp(6)    NOT NULL,

    CONSTRAINT articulos_lista_pkey PRIMARY KEY (id),
    CONSTRAINT fk_articulos_lista_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE
);

CREATE INDEX idx_articulos_lista_hogar ON articulos_lista (hogar_id);
