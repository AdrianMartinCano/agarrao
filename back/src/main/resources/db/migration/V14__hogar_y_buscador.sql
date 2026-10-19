-- ---------------------------------------------------------------------------
-- V14 · Hogar y bazar, y buscar en todo lo de las tiendas.
--
-- Listillo compara también lo que no se come: «si quiero comprar una fregona, quiero saber
-- dónde es más barata». Un pasillo nuevo (HOGAR) con sus genéricos, y el nombre de cada
-- producto de las webs normalizado (minúsculas, sin acentos) para buscar cualquier cosa.
-- ---------------------------------------------------------------------------

ALTER TABLE productos_tienda ADD COLUMN nombre_normalizado varchar(160);
UPDATE productos_tienda SET nombre_normalizado = lower(nombre);
ALTER TABLE productos_tienda ALTER COLUMN nombre_normalizado SET NOT NULL;
CREATE INDEX idx_productos_tienda_nombre ON productos_tienda (nombre_normalizado varchar_pattern_ops);

INSERT INTO genericos (id, hogar_id, nombre, categoria, unidad, pistas, retrato, creado_en)
SELECT gen_random_uuid(), NULL, g.nombre, g.categoria, g.unidad, g.pistas, g.retrato, now()
FROM (VALUES
    ('Fregona',                    'HOGAR', 'UD', 'mopa,recambio fregona,-palo,-cubo',              NULL),
    ('Palo de fregona',            'HOGAR', 'UD', 'palo fregona,mango,palo escoba',                 NULL),
    ('Cubo de fregar',             'HOGAR', 'UD', 'cubo fregona,cubo escurridor,cubo con escurridor', NULL),
    ('Escoba',                     'HOGAR', 'UD', 'cepillo barrer,-palo,-dientes',                  NULL),
    ('Recogedor',                  'HOGAR', 'UD', 'recogedor con mango',                            NULL),
    ('Bayetas',                    'HOGAR', 'UD', 'bayeta,bayeta microfibra',                       NULL),
    ('Estropajo',                  'HOGAR', 'UD', 'estropajos,salvauñas',                           NULL),
    ('Guantes de limpieza',        'HOGAR', 'UD', 'guantes,guantes latex,guantes nitrilo',          NULL),
    ('Pilas AA',                   'HOGAR', 'UD', 'pila aa,pilas alcalinas aa,lr6',                 NULL),
    ('Pilas AAA',                  'HOGAR', 'UD', 'pila aaa,pilas alcalinas aaa,lr03',              NULL),
    ('Bombillas LED',              'HOGAR', 'UD', 'bombilla led,bombilla',                          NULL),
    ('Servilletas de papel',       'HOGAR', 'UD', 'servilleta,servilletas',                         NULL),
    ('Film transparente',          'HOGAR', 'UD', 'film alimentario,papel film',                    NULL),
    ('Bolsas de congelación',      'HOGAR', 'UD', 'bolsas congelar,bolsa congelacion,bolsas zip',   NULL),
    ('Velas',                      'HOGAR', 'UD', 'vela,velas perfumadas',                          NULL),
    ('Pastillas lavavajillas',     'LIMPIEZA', 'UD', 'lavavajillas maquina,pastillas lavavajillas,capsulas lavavajillas', NULL),
    ('Friegasuelos',               'LIMPIEZA', 'L', 'limpiador suelos,fregasuelos',                 NULL),
    ('Limpiacristales',            'LIMPIEZA', 'L', 'limpia cristales',                             NULL),
    ('Ambientador',                'LIMPIEZA', 'UD', 'ambientador spray,ambientador electrico',     NULL)
) AS g (nombre, categoria, unidad, pistas, retrato)
WHERE NOT EXISTS (SELECT 1 FROM genericos x WHERE x.hogar_id IS NULL AND lower(x.nombre) = lower(g.nombre));
