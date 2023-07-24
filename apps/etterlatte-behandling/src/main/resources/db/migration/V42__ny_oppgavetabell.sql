
CREATE TABLE oppgave (
    id UUID PRIMARY KEY,
    status TEXT,
    enhet TEXT,
    sak_id BIGINT NOT NULL,
    saksbehandler TEXT,
    referanse TEXT,
    merknad TEXT,
    opprettet TIMESTAMP
);

CREATE INDEX ON oppgave(sak_id);
CREATE INDEX ON oppgave(saksbehandler);
