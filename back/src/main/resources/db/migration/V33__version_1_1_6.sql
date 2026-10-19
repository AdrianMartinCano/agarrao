-- V33 · Se anuncia la 1.1.6: el nombre ya no vuelve a ser el email al entrar.
UPDATE version_app
SET ultima = '1.1.6',
    notas = 'Tu nombre ya no se cambia por el email al volver a entrar.',
    actualizada_en = now()
WHERE id = 1;
