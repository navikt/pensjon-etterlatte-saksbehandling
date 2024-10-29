CREATE TABLE aktivitetsplikt_brevdata
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL,
    oppgave_id    UUID UNIQUE,
    aktivitetsgrad        TEXT,
    utbetaling           BOOLEAN,
    redusertEtterInntekt     BOOLEAN,
    nasjonalEllerUtland        TEXT,
    CONSTRAINT fk_sak_id FOREIGN KEY (sak_id) REFERENCES sak (id),
    CONSTRAINT fk_oppgave_id FOREIGN KEY (oppgave_id) REFERENCES oppgave (id)
);