-- ---------------------------------------------------------------------------
-- V19 · «No es esto» en los genéricos propios.
--
-- Un genérico propio («Solomillo de cerdo») recibe los productos de las webs buscando su
-- nombre. Lo que la casa descarta se apunta aquí, para ese genérico solo: no toca la
-- clasificación común de los productos, que es de todos.
-- ---------------------------------------------------------------------------

CREATE TABLE descartes_web (
    generico_id     uuid            NOT NULL REFERENCES genericos (id) ON DELETE CASCADE,
    cadena          varchar(20)     NOT NULL,
    id_externo      varchar(40)     NOT NULL,
    PRIMARY KEY (generico_id, cadena, id_externo)
);
