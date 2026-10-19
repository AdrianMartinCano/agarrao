-- ---------------------------------------------------------------------------
-- V18 · Más genéricos en el catálogo: lo que se compra mucho y faltaba.
--
-- El solomillo de cerdo solo se podía ver con el buscador. Ahora la carne, el pescado y la
-- verdura de todas las semanas tienen su genérico (y Eroski y Caprabo, que se leen buscando
-- cada genérico, también los traen). Las pistas siguen la regla de la primera palabra: lo
-- que es la cosa va delante («Solomillo de cerdo ibérico»).
-- ---------------------------------------------------------------------------

INSERT INTO genericos (id, hogar_id, nombre, categoria, unidad, pistas, retrato, creado_en)
SELECT gen_random_uuid(), NULL, g.nombre, g.categoria, g.unidad, g.pistas, NULL, now()
FROM (VALUES
    -- Carne
    ('Solomillo de cerdo',     'CARNE',      'KG', 'solomillo cerdo,solomillo iberico,solomillo'),
    ('Chuletas de cerdo',      'CARNE',      'KG', 'chuleta cerdo,chuletas cerdo,chuleta aguja,chuleta lomo,-cordero'),
    ('Costillas de cerdo',     'CARNE',      'KG', 'costilla cerdo,costillas cerdo,costilla,costillas,-cordero'),
    ('Secreto ibérico',        'CARNE',      'KG', 'secreto,secreto iberico'),
    ('Muslos de pollo',        'CARNE',      'KG', 'muslo pollo,muslos pollo,contramuslo,contramuslos,jamoncitos pollo'),
    ('Alitas de pollo',        'CARNE',      'KG', 'alitas,alas pollo,alitas pollo'),
    ('Chuletas de cordero',    'CARNE',      'KG', 'chuleta cordero,chuletas cordero,chuletillas cordero,chuletillas'),
    ('Bacon',                  'CARNE',      'KG', 'bacon,beicon,panceta ahumada,en:bacon'),
    ('Fuet',                   'CARNE',      'KG', 'fuet,espetec'),
    ('Salchichón',             'CARNE',      'KG', 'salchichon'),
    -- Pescado
    ('Bacalao',                'PESCADO',    'KG', 'bacalao,lomos bacalao,-croqueta'),
    ('Mejillones',             'PESCADO',    'KG', 'mejillon,mejillones,-escabeche'),
    ('Calamares',              'PESCADO',    'KG', 'calamar,calamares,anillas calamar,-tinta'),
    ('Dorada',                 'PESCADO',    'KG', 'dorada'),
    ('Lubina',                 'PESCADO',    'KG', 'lubina'),
    ('Surimi',                 'PESCADO',    'KG', 'surimi,palitos cangrejo,palitos mar'),
    -- Lácteos
    ('Queso de untar',         'LACTEOS',    'KG', 'queso untar,crema queso,queso crema,en:cream-cheeses'),
    ('Mozzarella',             'LACTEOS',    'KG', 'mozzarella,queso mozzarella,en:mozzarella'),
    ('Natillas',               'LACTEOS',    'KG', 'natillas,natilla'),
    -- Despensa
    ('Vinagre',                'DESPENSA',   'L',  'vinagre,en:vinegars'),
    ('Ketchup',                'DESPENSA',   'KG', 'ketchup,tomate ketchup,en:ketchup'),
    ('Maíz dulce',             'DESPENSA',   'KG', 'maiz dulce,maiz,en:sweet-corns'),
    ('Caldo',                  'DESPENSA',   'L',  'caldo,caldo pollo,caldo verduras,caldo cocido,-pastillas,-concentrado,en:broths'),
    ('Miel',                   'DESPENSA',   'KG', 'miel,en:honeys'),
    ('Copos de avena',         'DESAYUNO',   'KG', 'copos avena,avena,en:rolled-oats'),
    -- Panadería
    ('Magdalenas',             'PANADERIA',  'KG', 'magdalena,magdalenas,en:madeleines'),
    ('Croissants',             'PANADERIA',  'KG', 'croissant,croissants,cruasan,cruasanes,en:croissants'),
    -- Bebidas
    ('Refresco de naranja',    'BEBIDAS',    'L',  'refresco naranja,naranja con gas,fanta naranja,kas naranja'),
    ('Vino blanco',            'BEBIDAS',    'L',  'vino blanco,en:white-wines'),
    -- Fruta
    ('Kiwis',                  'FRUTA',      'KG', 'kiwi,kiwis'),
    ('Melocotones',            'FRUTA',      'KG', 'melocoton,melocotones,-almibar,-zumo'),
    ('Piña',                   'FRUTA',      'KG', 'pina,-almibar,-zumo,-lata'),
    -- Verdura
    ('Espinacas',              'VERDURA',    'KG', 'espinaca,espinacas'),
    ('Champiñones',            'VERDURA',    'KG', 'champinon,champinones,champinon laminado'),
    ('Judías verdes',          'VERDURA',    'KG', 'judia verde,judias verdes,judia redonda,judia plana'),
    ('Berenjenas',             'VERDURA',    'KG', 'berenjena,berenjenas'),
    ('Puerros',                'VERDURA',    'KG', 'puerro,puerros'),
    -- Congelados
    ('Croquetas',              'CONGELADOS', 'KG', 'croqueta,croquetas'),
    ('Varitas de pescado',     'CONGELADOS', 'KG', 'varitas,varitas merluza,palitos merluza,barritas merluza,en:fish-sticks'),
    -- Higiene
    ('Jabón de manos',         'HIGIENE',    'L',  'jabon manos,gel manos,jabon liquido,-lavavajillas,-ropa'),
    ('Pañales',                'HIGIENE',    'UD', 'panal,panales,en:diapers')
) AS g (nombre, categoria, unidad, pistas)
WHERE NOT EXISTS (SELECT 1 FROM genericos x WHERE x.hogar_id IS NULL AND lower(x.nombre) = lower(g.nombre));

-- Lo que ahora tiene genérico propio deja de ser pista de otro.
UPDATE genericos SET pistas = 'en:chorizos,chorizo'
WHERE hogar_id IS NULL AND nombre = 'Chorizo';

UPDATE genericos SET pistas = 'pollo entero,pollo troceado,-carcasa,-espinazo,-caldo,-higado,-mollejas,-muslo,-contramuslo,-alas,-alitas'
WHERE hogar_id IS NULL AND nombre = 'Pollo';
