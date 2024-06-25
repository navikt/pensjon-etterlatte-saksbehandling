CREATE TABLE reguleringskonfigurasjon
(
    id                SERIAL PRIMARY KEY,
    opprettet         TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    endret            TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    versjon           INTEGER                  NOT NULL DEFAULT 1,
    antall            INTEGER                  NOT NULL DEFAULT -1,
    dato              DATE                     NOT NULL,
    spesifikke_saker  BIGINT[],
    ekskluderte_saker BIGINT[],
    aktiv             BOOLEAN                  NOT NULL DEFAULT TRUE
);