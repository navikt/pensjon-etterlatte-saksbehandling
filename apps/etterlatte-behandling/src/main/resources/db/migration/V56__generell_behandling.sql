CREATE TABLE generellbehandling
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL,
    type          TEXT,
    innhold       JSONB,
    opprettet     TIMESTAMP
)