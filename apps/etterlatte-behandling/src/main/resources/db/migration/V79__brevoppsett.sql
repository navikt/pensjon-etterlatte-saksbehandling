create table brevoppsett
(
    behandling_id     UUID PRIMARY KEY
        CONSTRAINT brevoppsett_behandling_fk_id REFERENCES behandling (id),
    oppdatert         TIMESTAMP,
    etterbetaling_fom DATE,
    etterbetaling_tom DATE,
    brevtype          TEXT,
    aldersgruppe      TEXT,
    kilde             TEXT
)