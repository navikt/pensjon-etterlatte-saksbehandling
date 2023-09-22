create table tilbakekreving
(
    id          UUID
        PRIMARY KEY,
    status      TEXT,
    sak_id      BIGINT NOT NULL
        CONSTRAINT tilbakekreving_sak_fk_id
            REFERENCES sak (id),
    opprettet   TIMESTAMP,
    kravgrunnlag JSONB
)