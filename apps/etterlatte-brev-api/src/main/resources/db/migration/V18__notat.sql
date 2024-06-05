CREATE TABLE notat
(
    id BIGSERIAL
        CONSTRAINT notat_pk
            PRIMARY KEY,
    sak_id BIGINT NOT NULL,
    journalpost_id TEXT,
    tittel TEXT NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    payload TEXT NOT NULL,
    bytes   BYTEA
);

CREATE TABLE notat_hendelse
(
    id BIGSERIAL
        CONSTRAINT notat_hendelse_pk
            PRIMARY KEY,
    notat_id BIGINT NOT NULL
        CONSTRAINT notat_id_fk
            REFERENCES notat (id)
            ON DELETE CASCADE,
    saksbehandler TEXT NOT NULL,
    payload TEXT NOT NULL,
    tidspunkt TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL
)