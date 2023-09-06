create table klage
(
    id          UUID
        PRIMARY KEY,
    sak_id      BIGINT NOT NULL
        CONSTRAINT klage_sak_fk_id
            REFERENCES sak (id),
    opprettet   TIMESTAMP,
    status      TEXT,
    kabalstatus TEXT,
    formkrav    JSONB,
    utfall      JSONB
)