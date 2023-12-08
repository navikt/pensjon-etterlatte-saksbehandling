create table brevutfall
(
    behandling_id     UUID PRIMARY KEY
        CONSTRAINT brevutfall_behandling_fk_id REFERENCES behandling (id),
    oppdatert         TIMESTAMP,
    etterbetaling_fom DATE,
    etterbetaling_tom DATE,
    aldersgruppe      TEXT,
    kilde             TEXT
)