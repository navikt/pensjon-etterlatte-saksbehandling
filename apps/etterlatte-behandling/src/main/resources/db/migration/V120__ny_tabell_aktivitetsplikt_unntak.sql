CREATE TABLE aktivitetsplikt_unntak
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL,
    behandling_id UUID UNIQUE,
    oppgave_id    UUID UNIQUE,
    unntak        TEXT   NOT NULL,
    tom           Date,
    opprettet     TEXT,
    endret        TEXT,
    beskrivelse   TEXT,
    CONSTRAINT fk_sak_id FOREIGN KEY (sak_id) REFERENCES sak (id),
    CONSTRAINT fk_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id),
    CONSTRAINT fk_oppgave_id FOREIGN KEY (oppgave_id) REFERENCES oppgave (id)
);

ALTER TABLE aktivitetsplikt_vurdering
    RENAME TO aktivitetsplikt_aktivitetsgrad;
ALTER TABLE aktivitetsplikt_aktivitetsgrad
    RENAME COLUMN vurdering TO aktivitetsgrad;