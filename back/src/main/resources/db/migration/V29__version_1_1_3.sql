-- V29 · Se anuncia la 1.1.3: Gasto ordena «Anotado este mes».
UPDATE version_app
SET ultima = '1.1.3',
    notas = 'Gasto: «Anotado este mes» más claro y los botones centrados.',
    actualizada_en = now()
WHERE id = 1;
