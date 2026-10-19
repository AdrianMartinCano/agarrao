-- V30 · Se anuncia la 1.1.4: Gasolina sin blancos.
UPDATE version_app
SET ultima = '1.1.4',
    notas = 'Gasolina en fichas de color, sin tarjetas blancas, y «Ordenar por» nuevo.',
    actualizada_en = now()
WHERE id = 1;
