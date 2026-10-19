-- V36 · Se anuncia la 1.2.2: páginas de privacidad y condiciones.
UPDATE version_app
SET ultima = '1.2.2',
    notas = 'Privacidad y condiciones de uso de Agarrao, enlazadas en la portada.',
    actualizada_en = now()
WHERE id = 1;
