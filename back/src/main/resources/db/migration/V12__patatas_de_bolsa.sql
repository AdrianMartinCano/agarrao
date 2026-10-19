-- ---------------------------------------------------------------------------
-- V12 · Las patatas de bolsa por su nombre en Lidl y Mercadona.
--
-- «Patatas lisas», «Patatas onduladas» o «Patatas extra crujientes» son patatas fritas de
-- bolsa, pero solo casaban con «Patatas fritas de bolsa» por la palabra «patatas» y no
-- llegaban al umbral. Las congeladas de Lidl («Patatas onduladas» en Alimentos congelados)
-- no se cuelan: Lidl ya da la sección y se busca solo en sus pasillos.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = pistas || ',patatas lisas,patatas onduladas,patatas extra crujientes,patatas campesinas'
WHERE hogar_id IS NULL AND nombre = 'Patatas fritas de bolsa';
