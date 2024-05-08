CREATE TABLE aktivitetsplikt_vurdering
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL,
    behandling_id UUID,
    oppgave_id    UUID,
    vurdering     TEXT   NOT NULL,
    fom           Date,
    opprettet     TEXT,
    endret        TEXT,
    beskrivelse   TEXT,
    CONSTRAINT fk_sak_id FOREIGN KEY (sak_id) REFERENCES sak (id),
    CONSTRAINT fk_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling (id),
    CONSTRAINT fk_oppgave_id FOREIGN KEY (oppgave_id) REFERENCES oppgave (id)
);