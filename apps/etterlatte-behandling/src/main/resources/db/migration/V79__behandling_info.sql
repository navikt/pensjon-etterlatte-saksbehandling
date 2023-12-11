create table behandling_info
(
    behandling_id     UUID PRIMARY KEY
        CONSTRAINT behandling_info_behandling_fk_id REFERENCES behandling (id),
    oppdatert         TIMESTAMP,
    etterbetaling_fom DATE,
    etterbetaling_tom DATE,
    aldersgruppe      TEXT,
    kilde             TEXT
)