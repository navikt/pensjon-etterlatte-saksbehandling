CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE jobb
(
    id                SERIAL PRIMARY KEY,
    opprettet         TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    endret            TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    versjon           INTEGER NOT NULL DEFAULT 1,
    type              TEXT                     NOT NULL,
    kjoeredato        DATE                     NOT NULL,
    behandlingsmaaned TEXT                     NOT NULL,
    dryrun            BOOLEAN                  NOT NULL DEFAULT FALSE,
    status            TEXT                     NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX ON jobb (type);
CREATE INDEX ON jobb (status);

CREATE TABLE hendelse
(
    id          UUID PRIMARY KEY DEFAULT (uuid_generate_v4()),
    jobb_id     INTEGER NOT NULL,
    sak_id      INTEGER NOT NULL,
    opprettet   TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    endret      TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    versjon     INTEGER NOT NULL DEFAULT 1,
    status      TEXT NOT NULL DEFAULT 'OPPRETTET',
    utfall      TEXT,
    info        JSONB,
    FOREIGN KEY (jobb_id) REFERENCES jobb (id)
);

CREATE INDEX ON hendelse (jobb_id);
CREATE INDEX ON hendelse (status);
