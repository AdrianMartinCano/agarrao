-- ---------------------------------------------------------------------------
-- V21 · Un personaje propio para los genéricos más comprados.
--
-- Hasta ahora casi todos llevaban el de su pasillo (todas las carnes, el muslito). Los
-- dibujos están en front/public/personajes y los genera front/tools/personajes_genericos.py.
-- Los que no salen aquí siguen con el de su pasillo.
-- ---------------------------------------------------------------------------

UPDATE genericos g SET retrato = r.retrato
FROM (VALUES
    ('Plátanos', 'platano.svg'),
    ('Manzanas', 'manzana.svg'),
    ('Naranjas', 'naranja.svg'),
    ('Mandarinas', 'naranja.svg'),
    ('Fresas', 'fresa.svg'),
    ('Tomates', 'tomate.svg'),
    ('Patatas', 'patata.svg'),
    ('Zanahorias', 'zanahoria.svg'),
    ('Lechuga', 'lechuga.svg'),
    ('Cebollas', 'cebolla.svg'),
    ('Aceite de oliva', 'aceite.svg'),
    ('Aceite de oliva virgen extra', 'aceite.svg'),
    ('Aceite de girasol', 'aceite.svg'),
    ('Arroz', 'arroz.svg'),
    ('Pasta', 'pasta.svg'),
    ('Atún en lata', 'atun.svg'),
    ('Yogur natural', 'yogur.svg'),
    ('Yogur griego', 'yogur.svg'),
    ('Yogur de sabores', 'yogur.svg'),
    ('Queso curado', 'queso.svg'),
    ('Queso en lonchas', 'queso.svg'),
    ('Queso rallado', 'queso.svg'),
    ('Mantequilla', 'mantequilla.svg'),
    ('Pollo', 'pollo.svg'),
    ('Pechuga de pollo', 'pollo.svg'),
    ('Jamón serrano', 'jamon.svg'),
    ('Jamón cocido', 'jamon.svg'),
    ('Salchichas', 'salchicha.svg'),
    ('Pan de molde', 'pan-molde.svg'),
    ('Pan tostado', 'pan-molde.svg'),
    ('Galletas', 'galleta.svg'),
    ('Café molido', 'cafe.svg'),
    ('Chocolate', 'chocolate.svg'),
    ('Zumo de naranja', 'zumo.svg'),
    ('Refresco de cola', 'refresco.svg'),
    ('Cerveza', 'cerveza.svg'),
    ('Vino tinto', 'vino.svg'),
    ('Vino blanco', 'vino.svg'),
    ('Pizza', 'pizza.svg'),
    ('Helado', 'helado.svg'),
    ('Detergente para ropa', 'detergente.svg')
) AS r (nombre, retrato)
WHERE g.hogar_id IS NULL AND g.nombre = r.nombre;
