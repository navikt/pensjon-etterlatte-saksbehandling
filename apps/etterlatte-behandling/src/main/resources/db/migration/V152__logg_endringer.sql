CREATE TABLE sakendringer
(
    id             UUID PRIMARY KEY,
    sakId          BIGINT NOT NULL,
    sakFoer        JSONB,
    sakEtter       JSONB,
    tidspunkt      TIMESTAMP,
    saksbehandler  TEXT NOT NULL,
    kallendeMetode TEXT NOT NULL
);