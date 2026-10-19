-- ---------------------------------------------------------------------------
-- V8 · Productos genéricos y su clasificación.
--
-- Un genérico es lo que se compra sin mirar marca ni tienda («Leche semidesnatada»,
-- «Huevos»). Se compara por precio por unidad (€/L, €/kg, €/ud): así la marca blanca de
-- cada cadena se compara con las demás aunque no compartan código de barras.
--
-- Listillo trae un catálogo fijo (hogar_id null) y cada hogar puede crear los suyos.
-- ---------------------------------------------------------------------------

CREATE TABLE genericos (
    id              uuid            NOT NULL,
    -- Null = del catálogo de Listillo, para todos. Si no, de ese hogar.
    hogar_id        uuid,
    nombre          varchar(60)     NOT NULL,
    categoria       varchar(20)     NOT NULL,
    -- En qué se compara: €/kg, €/L o €/ud.
    unidad          varchar(2)      NOT NULL CHECK (unidad IN ('UD', 'KG', 'L')),
    -- Pistas para adivinar, separadas por comas: categorías de Open Food Facts («en:eggs»)
    -- y otras formas de llamarlo en un ticket («semi», «york»).
    pistas          varchar(400),
    -- Dibujo propio en public/personajes; sin él, el de su categoría.
    retrato         varchar(40),
    creado_en       timestamp(6)    NOT NULL,

    CONSTRAINT genericos_pkey PRIMARY KEY (id),
    CONSTRAINT fk_genericos_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE
);

-- Sin nombres repetidos en el catálogo ni dentro de un hogar.
CREATE UNIQUE INDEX uq_genericos_catalogo_nombre ON genericos (lower(nombre)) WHERE hogar_id IS NULL;
CREATE UNIQUE INDEX uq_genericos_hogar_nombre ON genericos (hogar_id, lower(nombre)) WHERE hogar_id IS NOT NULL;

-- Qué genérico es cada producto de ticket. Un producto se identifica como lo hace el
-- aprendizaje de tickets: el texto (normalizado) en esa cadena. Es de cada hogar.
CREATE TABLE clasificaciones (
    hogar_id        uuid            NOT NULL,
    cadena          varchar(20)     NOT NULL,
    texto           varchar(120)    NOT NULL,
    -- Null con origen USUARIO = «no es ningún genérico»: no se vuelve a adivinar.
    generico_id     uuid,
    -- Cuánto trae una unidad comprada, en la unidad del genérico (6 para «6X1L» en €/L).
    -- Solo si alguien lo ha corregido; si no, se lee del producto o del texto del ticket.
    tamano          numeric(10, 3),
    origen          varchar(8)      NOT NULL CHECK (origen IN ('AUTO', 'USUARIO')),
    actualizado_en  timestamp(6)    NOT NULL,

    CONSTRAINT clasificaciones_pkey PRIMARY KEY (hogar_id, cadena, texto),
    CONSTRAINT fk_clasificaciones_hogar FOREIGN KEY (hogar_id) REFERENCES hogares (id) ON DELETE CASCADE,
    CONSTRAINT fk_clasificaciones_generico FOREIGN KEY (generico_id) REFERENCES genericos (id) ON DELETE CASCADE,
    CONSTRAINT clasificaciones_tamano_ck CHECK (tamano IS NULL OR tamano > 0)
);

CREATE INDEX idx_clasificaciones_generico ON clasificaciones (generico_id);

-- El catálogo de Listillo.
INSERT INTO genericos (id, hogar_id, nombre, categoria, unidad, pistas, retrato, creado_en)
SELECT gen_random_uuid(), NULL, g.nombre, g.categoria, g.unidad, g.pistas, g.retrato, now()
FROM (VALUES
    -- Lácteos y huevos
    ('Leche entera',               'LACTEOS',    'L',  'en:whole-milks,leche entera',                          'leche.svg'),
    ('Leche semidesnatada',        'LACTEOS',    'L',  'en:semi-skimmed-milks,leche semi',                     'leche.svg'),
    ('Leche desnatada',            'LACTEOS',    'L',  'en:skimmed-milks,leche desnat',                        'leche.svg'),
    ('Leche sin lactosa',          'LACTEOS',    'L',  'en:lactose-free-milks,leche sin lactosa',              'leche.svg'),
    ('Bebida de avena',            'LACTEOS',    'L',  'en:oat-based-drinks,bebida avena',                     NULL),
    ('Bebida de soja',             'LACTEOS',    'L',  'en:soy-based-drinks,bebida soja',                      NULL),
    ('Yogur natural',              'LACTEOS',    'KG', 'en:plain-yogurts,yogur natural',                       NULL),
    ('Yogur de sabores',           'LACTEOS',    'KG', 'en:flavoured-yogurts,yogur sabor,yogur fresa,yogur limon', NULL),
    ('Yogur griego',               'LACTEOS',    'KG', 'en:greek-style-yogurts,griego',                        NULL),
    ('Queso fresco',               'LACTEOS',    'KG', 'en:fresh-cheeses,queso burgos,queso fresco',           NULL),
    ('Queso curado',               'LACTEOS',    'KG', 'queso curado,queso semicurado,manchego',               NULL),
    ('Queso en lonchas',           'LACTEOS',    'KG', 'queso lonchas,lonchas queso',                          NULL),
    ('Queso rallado',              'LACTEOS',    'KG', 'en:grated-cheese,queso rallado',                       NULL),
    ('Mantequilla',                'LACTEOS',    'KG', 'en:butters',                                           NULL),
    ('Nata para cocinar',          'LACTEOS',    'L',  'en:cooking-creams,nata cocinar,nata',                  NULL),
    ('Huevos',                     'LACTEOS',    'UD', 'en:eggs,huevo,huevos camperos,docena huevos',          'huevo.svg'),
    -- Pan
    ('Pan de barra',               'PANADERIA',  'UD', 'barra pan,baguette,pan barra',                     NULL),
    ('Pan de molde',               'PANADERIA',  'KG', 'en:sliced-breads,pan molde,molde',                     NULL),
    ('Pan tostado',                'PANADERIA',  'KG', 'en:toasts,tostadas,biscottes',                         NULL),
    -- Fruta
    ('Plátanos',                   'FRUTA',      'KG', 'platano,banana',                                       NULL),
    ('Manzanas',                   'FRUTA',      'KG', 'manzana golden,manzana gala,manzana fuji',             NULL),
    ('Naranjas',                   'FRUTA',      'KG', 'naranja mesa,naranja zumo',                            NULL),
    ('Mandarinas',                 'FRUTA',      'KG', 'clementina',                                           NULL),
    ('Peras',                      'FRUTA',      'KG', 'pera conferencia',                                     NULL),
    ('Fresas',                     'FRUTA',      'KG', 'freson',                                               NULL),
    ('Uvas',                       'FRUTA',      'KG', 'uva blanca,uva negra',                                 NULL),
    ('Limones',                    'FRUTA',      'KG', 'limon',                                                NULL),
    ('Aguacates',                  'FRUTA',      'KG', 'aguacate',                                             NULL),
    ('Sandía',                     'FRUTA',      'KG', 'sandia',                                               NULL),
    ('Melón',                      'FRUTA',      'KG', 'melon',                                                NULL),
    -- Verdura
    ('Tomates',                    'VERDURA',    'KG', 'tomate pera,tomate rama,tomate ensalada,tomate cherry', NULL),
    ('Patatas',                    'VERDURA',    'KG', 'patata,malla patata',                                  NULL),
    ('Cebollas',                   'VERDURA',    'KG', 'cebolla',                                              NULL),
    ('Ajos',                       'VERDURA',    'KG', 'ajo,cabeza ajo',                                       NULL),
    ('Zanahorias',                 'VERDURA',    'KG', 'zanahoria',                                            NULL),
    ('Lechuga',                    'VERDURA',    'UD', 'lechuga iceberg,cogollos,ensalada bolsa',              NULL),
    ('Pimientos',                  'VERDURA',    'KG', 'pimiento rojo,pimiento verde,pimiento italiano',       NULL),
    ('Calabacín',                  'VERDURA',    'KG', 'calabacin',                                            NULL),
    ('Pepino',                     'VERDURA',    'KG', 'pepinos',                                              NULL),
    ('Brócoli',                    'VERDURA',    'KG', 'brocoli',                                              NULL),
    -- Carne
    ('Pechuga de pollo',           'CARNE',      'KG', 'pechuga pollo,filete pechuga,pechuga',                 NULL),
    ('Pollo',                      'CARNE',      'KG', 'muslos pollo,contramuslo,alas pollo,pollo entero',     NULL),
    ('Carne picada',               'CARNE',      'KG', 'picada,burger meat,hamburguesa',                       NULL),
    ('Filetes de ternera',         'CARNE',      'KG', 'ternera filete,filete ternera,ternera',                NULL),
    ('Lomo de cerdo',              'CARNE',      'KG', 'lomo cerdo,cinta lomo,lomo',                           NULL),
    ('Jamón cocido',               'CARNE',      'KG', 'en:cooked-hams,jamon york,york,jamon cocido',          NULL),
    ('Pavo en lonchas',            'CARNE',      'KG', 'pechuga pavo,pavo',                                    NULL),
    ('Jamón serrano',              'CARNE',      'KG', 'en:serrano-hams,jamon serrano,serrano',                NULL),
    ('Chorizo',                    'CARNE',      'KG', 'en:chorizos,salchichon,fuet',                          NULL),
    ('Salchichas',                 'CARNE',      'KG', 'en:sausages,salchicha,frankfurt',                      NULL),
    -- Pescado
    ('Salmón',                     'PESCADO',    'KG', 'salmon',                                               NULL),
    ('Merluza',                    'PESCADO',    'KG', 'merluza filete',                                       NULL),
    ('Gambas',                     'PESCADO',    'KG', 'gamba,langostino',                                     NULL),
    ('Atún en lata',               'PESCADO',    'KG', 'en:canned-tunas,atun,atun claro',                      NULL),
    -- Despensa
    ('Aceite de oliva virgen extra','DESPENSA',  'L',  'en:extra-virgin-olive-oils,aceite virgen extra,aceite oliva extra,aove', NULL),
    ('Aceite de oliva',            'DESPENSA',   'L',  'en:olive-oils,aceite oliva',                           NULL),
    ('Aceite de girasol',          'DESPENSA',   'L',  'en:sunflower-oils,aceite girasol',                     NULL),
    ('Arroz',                      'DESPENSA',   'KG', 'en:rices,arroz redondo,arroz largo,arroz bomba',       NULL),
    ('Pasta',                      'DESPENSA',   'KG', 'en:pastas,macarrones,espaguetis,spaghetti,tallarines,fideos,hélices,lazos', NULL),
    ('Garbanzos',                  'DESPENSA',   'KG', 'en:chickpeas,garbanzo',                                NULL),
    ('Lentejas',                   'DESPENSA',   'KG', 'en:lentils,lenteja',                                   NULL),
    ('Alubias',                    'DESPENSA',   'KG', 'en:white-beans,judias blancas,alubia',                 NULL),
    ('Harina',                     'DESPENSA',   'KG', 'en:flours,harina trigo',                               NULL),
    ('Azúcar',                     'DESPENSA',   'KG', 'en:sugars,azucar blanco,azucar moreno,azucar',         NULL),
    ('Sal',                        'DESPENSA',   'KG', 'en:salts,sal fina,sal gruesa,sal marina',              NULL),
    ('Tomate frito',               'DESPENSA',   'KG', 'en:tomato-sauces,tomate frito',                        'tomate-frito.svg'),
    ('Tomate triturado',           'DESPENSA',   'KG', 'en:crushed-tomatoes,tomate triturado',                 NULL),
    ('Aceitunas',                  'DESPENSA',   'KG', 'en:olives,aceituna',                                   NULL),
    ('Patatas fritas de bolsa',    'DESPENSA',   'KG', 'en:crisps,patatas fritas,chips',                       NULL),
    ('Mayonesa',                   'DESPENSA',   'KG', 'en:mayonnaises',                                       NULL),
    -- Desayuno y dulces
    ('Café molido',                'DESAYUNO',   'KG', 'en:ground-coffees,cafe molido,cafe',                   NULL),
    ('Cacao en polvo',             'DESAYUNO',   'KG', 'en:cocoa-and-chocolate-powders,cacao soluble,cacao',   NULL),
    ('Galletas',                   'DESAYUNO',   'KG', 'en:biscuits,galleta',                                  NULL),
    ('Cereales de desayuno',       'DESAYUNO',   'KG', 'en:breakfast-cereals,cereales,copos avena,muesli',     NULL),
    ('Chocolate',                  'DESAYUNO',   'KG', 'en:chocolates,chocolate negro,chocolate leche',        NULL),
    ('Mermelada',                  'DESAYUNO',   'KG', 'en:jams,confitura',                                    NULL),
    -- Bebidas
    ('Agua',                       'BEBIDAS',    'L',  'en:waters,agua mineral',                               NULL),
    ('Refresco de cola',           'BEBIDAS',    'L',  'en:colas,cola',                                        NULL),
    ('Zumo de naranja',            'BEBIDAS',    'L',  'en:orange-juices,zumo naranja',                        NULL),
    ('Cerveza',                    'BEBIDAS',    'L',  'en:beers,cerveza lata,cerveza botellin',               NULL),
    ('Vino tinto',                 'BEBIDAS',    'L',  'en:red-wines,tinto',                                   NULL),
    -- Congelados
    ('Pizza',                      'CONGELADOS', 'KG', 'en:pizzas',                                            NULL),
    ('Verduras congeladas',        'CONGELADOS', 'KG', 'en:frozen-vegetables,menestra,guisantes congelados',   NULL),
    ('Helado',                     'CONGELADOS', 'L',  'en:ice-creams,helados',                                NULL),
    -- Limpieza
    ('Detergente para ropa',       'LIMPIEZA',   'L',  'en:laundry-detergents,detergente',                     NULL),
    ('Suavizante',                 'LIMPIEZA',   'L',  'en:fabric-softeners',                                  NULL),
    ('Lavavajillas a mano',        'LIMPIEZA',   'L',  'lavavajillas,lavavajillas mano',                            NULL),
    ('Lejía',                      'LIMPIEZA',   'L',  'lejia',                                                NULL),
    ('Papel de cocina',            'LIMPIEZA',   'UD', 'rollo cocina,papel cocina',                            NULL),
    ('Bolsas de basura',           'LIMPIEZA',   'UD', 'bolsa basura',                                         NULL),
    -- Higiene
    ('Papel higiénico',            'HIGIENE',    'UD', 'en:toilet-papers,papel higienico',                     NULL),
    ('Gel de ducha',               'HIGIENE',    'L',  'en:shower-gels,gel ducha',                         NULL),
    ('Champú',                     'HIGIENE',    'L',  'en:shampoos,champu',                                   NULL),
    ('Pasta de dientes',           'HIGIENE',    'L',  'en:toothpastes,dentifrico,pasta dental',               NULL),
    ('Desodorante',                'HIGIENE',    'UD', 'en:deodorants',                                        NULL)
) AS g (nombre, categoria, unidad, pistas, retrato);
