CREATE TABLE pesyssak
(
    id                  UUID PRIMARY KEY,
    opprettet           TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    sak                 JSONB NOT NULL,
    migrert             BOOLEAN DEFAULT FALSE
);
