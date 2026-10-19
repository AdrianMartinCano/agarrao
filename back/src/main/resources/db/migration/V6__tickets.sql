-- ---------------------------------------------------------------------------
-- V6 · Tickets de compra.
--
-- Cada ticket guarda sus líneas con el precio pagado: de ahí salen, más adelante, la
-- comparación entre tiendas y los gráficos de gasto. Las líneas casadas con un producto
-- enseñan a Listillo qué quiere decir cada texto en cada cadena (alias_ticket).
-- ---------------------------------------------------------------------------

CREATE TABLE tickets (
    id              uuid            NOT NULL,
    hogar_id        uuid            NOT NULL,
    cadena          varchar(20)     NOT NULL,
    -- Lo que pone la cabecera («MERCADONA, S.A. A-46103834»), para enseñarlo tal cual.
    tienda_texto    varchar(80),
    fecha           date            NOT NULL,
    total           numeric(10, 2)  NOT NULL,
    origen          varchar(8)      NOT NULL CHECK (origen IN ('PDF', 'FOTO')),
    subido_por      varchar(36)     NOT NULL,
    creado_en       timestamp(6)    NOT NULL,

    CONSTRAINT tickets_pkey PRIMARY KEY (id),
    CONSTRAINT fk_tickets_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE
);

CREATE INDEX idx_tickets_hogar_fecha ON tickets (hogar_id, fecha DESC);

CREATE TABLE lineas_ticket (
    id              uuid            NOT NULL,
    ticket_id       uuid            NOT NULL,
    orden           smallint        NOT NULL,
    -- Lo que pone el ticket («LECHE SEMI HACEND 6X1L»).
    texto           varchar(120)    NOT NULL,
    cantidad        numeric(10, 3)  NOT NULL,
    unidad          varchar(2)      NOT NULL CHECK (unidad IN ('UD', 'KG', 'L')),
    precio_unitario numeric(10, 3),
    importe         numeric(10, 2)  NOT NULL,
    -- Con qué se casó: un producto con código de barras, o solo un nombre de la lista.
    ean             varchar(14),
    nombre          varchar(120),

    CONSTRAINT lineas_ticket_pkey PRIMARY KEY (id),
    CONSTRAINT fk_lineas_ticket_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE
);

CREATE INDEX idx_lineas_ticket_ticket ON lineas_ticket (ticket_id);
CREATE INDEX idx_lineas_ticket_ean ON lineas_ticket (ean) WHERE ean IS NOT NULL;

-- Lo aprendido, para todos: en esta cadena, este texto de ticket es este producto.
CREATE TABLE alias_ticket (
    cadena          varchar(20)     NOT NULL,
    texto           varchar(120)    NOT NULL,   -- normalizado: minúsculas, sin acentos
    ean             varchar(14),
    nombre          varchar(120)    NOT NULL,
    veces           integer         NOT NULL DEFAULT 1,
    actualizado_en  timestamp(6)    NOT NULL,

    CONSTRAINT alias_ticket_pkey PRIMARY KEY (cadena, texto)
);
