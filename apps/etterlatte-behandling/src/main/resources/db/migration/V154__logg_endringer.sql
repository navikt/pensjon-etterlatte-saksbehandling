CREATE TABLE endringer
(
    id             UUID PRIMARY KEY,
    tabell         TEXT   NOT NULL,
    foer           JSONB,
    etter          JSONB,
    tidspunkt      TIMESTAMP,
    saksbehandler  TEXT   NOT NULL,
    kallendeMetode TEXT   NOT NULL
);