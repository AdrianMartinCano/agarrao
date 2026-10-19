-- ---------------------------------------------------------------------------
-- V9 · Precios de las tiendas, leídos de sus webs.
--
-- El catálogo de cada cadena con su precio y su precio por unidad, para que Precios tenga
-- datos sin esperar a que el hogar suba tickets. Se importa cada noche y SOLO si está
-- encendido (listillo.precios-web.activo): leer las webs de los súper tiene riesgo legal si
-- la app se publica. Es de todos los hogares.
-- ---------------------------------------------------------------------------

CREATE TABLE productos_tienda (
    cadena          varchar(20)     NOT NULL,
    -- El id del producto en la web de la cadena («4241» en Mercadona).
    id_externo      varchar(40)     NOT NULL,
    nombre          varchar(160)    NOT NULL,
    -- Cómo se vende («Garrafa 5 L», «Paquete 1 kg»), para enseñarlo.
    formato         varchar(80),
    precio          numeric(10, 2)  NOT NULL,
    -- Lo que trae, en kilos, litros o piezas, y el precio por esa unidad.
    tamano          numeric(10, 3)  NOT NULL,
    unidad          varchar(2)      NOT NULL CHECK (unidad IN ('UD', 'KG', 'L')),
    precio_unidad   numeric(10, 3)  NOT NULL,
    -- La sección de la web («Huevos, leche y mantequilla»): ayuda a adivinar el genérico.
    seccion         varchar(120),
    imagen_url      varchar(500),
    url             varchar(500),
    generico_id     uuid,
    -- Corregido a mano: la importación no lo vuelve a adivinar.
    generico_manual boolean         NOT NULL DEFAULT false,
    actualizado_en  timestamp(6)    NOT NULL,

    CONSTRAINT productos_tienda_pkey PRIMARY KEY (cadena, id_externo),
    CONSTRAINT fk_productos_tienda_generico FOREIGN KEY (generico_id) REFERENCES genericos (id) ON DELETE SET NULL,
    CONSTRAINT productos_tienda_precio_ck CHECK (precio > 0 AND tamano > 0 AND precio_unidad > 0)
);

CREATE INDEX idx_productos_tienda_generico ON productos_tienda (generico_id) WHERE generico_id IS NOT NULL;
