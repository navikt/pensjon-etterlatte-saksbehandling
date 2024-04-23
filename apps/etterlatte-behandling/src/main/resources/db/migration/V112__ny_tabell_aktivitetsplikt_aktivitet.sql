CREATE TABLE aktivitetsplikt_aktivitet
(
    id             UUID PRIMARY KEY,
    behandling_id  UUID, -- skal kunne kobles til kun sak i fremtiden
    sak_id         BIGINT NOT NULL,
    aktivitet_type TEXT   NOT NULL,
    fom            Date,
    tom            Date,
    opprettet      TEXT,
    endret         TEXT,
    beskrivelse    TEXT
);