-- ---------------------------------------------------------------------------
-- V20 · Afinar tres genéricos de V18 con lo que traen de verdad las webs.
--
-- «Solomillo» a secas casaba con el de pavo, pollo, añojo o salmón; «Costilla» con la de
-- añojo o vacuno; «Piña» con la de lata en su jugo.
-- ---------------------------------------------------------------------------

UPDATE genericos SET pistas = 'solomillo cerdo,solomillo iberico,-pavo,-pollo,-anojo,-ternera,-vacuno,-salmon,-atun,-bacalao'
WHERE hogar_id IS NULL AND nombre = 'Solomillo de cerdo';

UPDATE genericos SET pistas = 'costilla cerdo,costillas cerdo,costilla iberico,-cordero,-anojo,-vacuno,-ternera,-patatas,-lata'
WHERE hogar_id IS NULL AND nombre = 'Costillas de cerdo';

UPDATE genericos SET pistas = 'pina,pina natural,-almibar,-zumo,-lata,-jugo'
WHERE hogar_id IS NULL AND nombre = 'Piña';
