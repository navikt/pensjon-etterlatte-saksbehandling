CREATE TABLE omregningskjoering
(
    id        UUID UNIQUE              NOT NULL DEFAULT gen_random_uuid(),
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kjoering  VARCHAR                  NOT NULL,
    status    VARCHAR                           DEFAULT 'KLAR',
    sak_id    BIGINT                   NOT NULL
        CONSTRAINT kjoering_sak_id_fk
            REFERENCES sak (id),
    PRIMARY KEY (id)
)