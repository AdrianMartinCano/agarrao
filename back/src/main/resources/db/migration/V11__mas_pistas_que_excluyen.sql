-- ---------------------------------------------------------------------------
-- V11 · Más pistas que excluyen, vistas con lo que publica Lidl.
--
-- Lidl no da secciones de alimentación, así que el genérico se busca en todos los pasillos
-- de comida y salen cruces: «Patatas lisas» (de bolsa) como Patatas, café en grano o soluble
-- como Café molido, galletas Tosta Rica como Pan tostado.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = pistas || ',-lisas,-onduladas,-fritas,-chips,-gajo,-bravas'
WHERE hogar_id IS NULL AND nombre = 'Patatas';

UPDATE genericos SET pistas = pistas || ',-grano,-soluble,-capsulas,-capsula,-monodosis'
WHERE hogar_id IS NULL AND nombre = 'Café molido';

UPDATE genericos SET pistas = pistas || ',-galletas'
WHERE hogar_id IS NULL AND nombre = 'Pan tostado';
