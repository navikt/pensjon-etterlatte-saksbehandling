CREATE TABLE brev
(
    id BIGSERIAL
        CONSTRAINT brev_pk
            PRIMARY KEY,
    behandling_id BIGINT NOT NULL,
    journalpost_id TEXT NULL,
    bestilling_id TEXT NULL,
    tittel TEXT NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL
);

CREATE TABLE mottaker
(
    brev_id BIGINT NOT NULL
        PRIMARY KEY
            REFERENCES brev (id)
                ON DELETE CASCADE,
    fornavn TEXT NOT NULL,
    etternavn TEXT NOT NULL,
    foedselsnummer TEXT DEFAULT NULL,
    adresse TEXT NOT NULL,
    postnummer TEXT NOT NULL,
    poststed TEXT NOT NULL,
    land TEXT DEFAULT NULL
);

CREATE TABLE innhold
(
    brev_id BIGINT NOT NULL
        PRIMARY KEY
            REFERENCES brev (id)
                ON DELETE CASCADE,
    mal TEXT,
    spraak TEXT,
    bytes BYTEA NOT NULL
);

CREATE TABLE status
(
    id VARCHAR(24)
        CONSTRAINT status_pk
            PRIMARY KEY,
    rang INT NOT NULL
);

INSERT INTO status(id, rang) VALUES('OPPRETTET', 1);
INSERT INTO status(id, rang) VALUES('OPPDATERT', 2);
INSERT INTO status(id, rang) VALUES('FERDIGSTILT', 3);
INSERT INTO status(id, rang) VALUES('JOURNALFOERT', 4);
INSERT INTO status(id, rang) VALUES('DISTRIBUERT', 5);
INSERT INTO status(id, rang) VALUES('SLETTET', 6);

CREATE TABLE hendelse
(
    id BIGSERIAL
        CONSTRAINT hendelse_pk
            PRIMARY KEY,
    brev_id BIGINT NOT NULL
        CONSTRAINT brev_id_fk
            REFERENCES brev (id)
                ON DELETE CASCADE,
    status_id VARCHAR(24)
        CONSTRAINT status_id_fk
            REFERENCES status (id),
    payload TEXT NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL
);
