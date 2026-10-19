-- V38 · Se anuncia la 1.2.4: estadísticas de visitas con Umami (sin cookies).
UPDATE version_app
SET ultima = '1.2.4',
    notas = 'Agarrao cuenta las visitas, sin cookies y en un servidor propio.',
    actualizada_en = now()
WHERE id = 1;
