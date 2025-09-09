CREATE TYPE etteroppgjoer_filter AS ENUM ('ENKEL');

CREATE TABLE etteroppgjoer_konfigurasjon (
    id                SERIAL PRIMARY KEY,
    opprettet         TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    endret            TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    versjon           INTEGER                  NOT NULL DEFAULT 1,
    antall            INTEGER                  NOT NULL DEFAULT -1,
    dato              DATE                     NOT NULL,
    etteroppgjoer_filter etteroppgjoer_filter NOT NULL DEFAULT 'ENKEL',
    spesifikke_saker  BIGINT[],
    ekskluderte_saker BIGINT[],
    aktiv             BOOLEAN                  NOT NULL DEFAULT TRUE,
    kjoering_id       VARCHAR
)