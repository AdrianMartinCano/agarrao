-- ---------------------------------------------------------------------------
-- V4 · Productos por código de barras.
--
-- Caché propia de Open Food Facts (que pide no repetir consultas: 15 por minuto por IP)
-- y los productos que OFF no conoce y alguien de Listillo nombró a mano. Se comparte
-- entre todos los hogares: un código nombrado una vez sirve para todos.
-- ---------------------------------------------------------------------------

CREATE TABLE productos (
    -- EAN-8, UPC-A (12), EAN-13 o GTIN-14, con el dígito de control comprobado.
    ean             varchar(14)     NOT NULL,
    -- Null solo en DESCONOCIDO: OFF no lo tiene y nadie lo ha nombrado todavía.
    nombre          varchar(120),
    marca           varchar(80),
    -- Tal cual lo da OFF o lo escribe la persona («1 L», «6 x 125 g»). El número y la
    -- unidad para comparar por €/unidad llegan con los genéricos.
    cantidad        varchar(40),
    imagen_url      varchar(500),
    -- Las primeras categorías de OFF (en:semi-skimmed-milks…), separadas por comas.
    categorias      varchar(500),
    origen          varchar(16)     NOT NULL CHECK (origen IN ('OPEN_FOOD_FACTS', 'USUARIO', 'DESCONOCIDO')),
    -- Cuándo se preguntó a OFF por última vez: para no volver a preguntar en un tiempo.
    consultado_en   timestamp(6)    NOT NULL,
    nombrado_por    varchar(36),

    CONSTRAINT productos_pkey PRIMARY KEY (ean),
    CONSTRAINT productos_nombre_ck CHECK (origen = 'DESCONOCIDO' OR nombre IS NOT NULL)
);

-- El artículo de la lista recuerda de qué envase salió (para comparar precios después).
-- Sin FK obligatoria hacia productos: el producto puede llegar a borrarse de la caché.
ALTER TABLE articulos_lista ADD COLUMN ean varchar(14);
