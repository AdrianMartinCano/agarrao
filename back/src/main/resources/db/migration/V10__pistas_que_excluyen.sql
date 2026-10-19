-- ---------------------------------------------------------------------------
-- V10 · Pistas que excluyen.
--
-- Con el catálogo web de Mercadona (miles de productos) salieron casados que no lo son:
-- huevos de codorniz como «Huevos», carcasa de pollo como «Pollo», papel higiénico húmedo
-- como «Papel higiénico». Una pista que empieza por «-» dice que con esa palabra NO es ese
-- genérico. Y el desodorante se vende por mililitros, no por piezas: se compara en €/L.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = pistas || ',-codorniz,-pasteurizado,-clara'
WHERE hogar_id IS NULL AND nombre = 'Huevos';

UPDATE genericos SET pistas = pistas || ',-carcasa,-espinazo,-caldo,-higado,-mollejas'
WHERE hogar_id IS NULL AND nombre = 'Pollo';

UPDATE genericos SET pistas = pistas || ',-humedo,-toallitas'
WHERE hogar_id IS NULL AND nombre = 'Papel higiénico';

UPDATE genericos SET pistas = pistas || ',-galletas,-cereales,-muesli,-gofres,-rosquillas,-bizcocho,-helado,-batido'
WHERE hogar_id IS NULL AND nombre = 'Chocolate';

UPDATE genericos SET pistas = pistas || ',-caldo,-pastillas'
WHERE hogar_id IS NULL AND nombre IN ('Pechuga de pollo', 'Carne picada');

UPDATE genericos SET unidad = 'L'
WHERE hogar_id IS NULL AND nombre = 'Desodorante';
