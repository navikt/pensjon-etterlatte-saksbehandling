CREATE TABLE grunnlagsendringshendelse
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL
        CONSTRAINT grlaghendelse_sak_id_fk
            REFERENCES sak (id),
    type          TEXT,
    opprettet     TIMESTAMP,
    data          JSONB,
    status        TEXT   NOT NULL,
    behandling_id UUID references behandling (id)
)