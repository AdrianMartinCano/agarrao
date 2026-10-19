-- ---------------------------------------------------------------------------
-- V15 · Patatas congeladas y hamburguesas, cada una en su sitio.
--
-- Vistos con el usuario (2026-10-10): las patatas prefritas o para horno (congeladas) se
-- mezclaban con las patatas frescas y con las de bolsa, y en «Carne picada» salían todas
-- las hamburguesas, porque el catálogo le puso «hamburguesa» y «burger meat» como pistas.
-- Ahora tienen genérico propio y los demás las excluyen.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = pistas || ',-prefritas,-congelad,-horno,-gajo,-deluxe'
WHERE hogar_id IS NULL AND nombre = 'Patatas';

UPDATE genericos SET pistas = pistas || ',-prefritas,-congelad,-horno,-gajo'
WHERE hogar_id IS NULL AND nombre = 'Patatas fritas de bolsa';

UPDATE genericos SET pistas = 'picada,carne picada,picada vacuno,picada cerdo,picada mixta,-hamburguesa,-burger,-ajo,-perejil'
WHERE hogar_id IS NULL AND nombre = 'Carne picada';

INSERT INTO genericos (id, hogar_id, nombre, categoria, unidad, pistas, retrato, creado_en)
SELECT gen_random_uuid(), NULL, g.nombre, g.categoria, g.unidad, g.pistas, NULL, now()
FROM (VALUES
    ('Patatas congeladas', 'CONGELADOS', 'KG', 'patatas prefritas,patatas congeladas,patatas horno,patatas gajo,patatas deluxe'),
    ('Hamburguesas',       'CARNE',      'KG', 'hamburguesa,burger meat,burger')
) AS g (nombre, categoria, unidad, pistas)
WHERE NOT EXISTS (SELECT 1 FROM genericos x WHERE x.hogar_id IS NULL AND lower(x.nombre) = lower(g.nombre));
