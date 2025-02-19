create table etteroppgjoer_behandling
(
    id        UUID
        PRIMARY KEY,
    status    TEXT,
    sak_id    BIGINT NOT NULL
        CONSTRAINT etteroppgjoer_behandling_sak_fk_id
            REFERENCES sak (id),
    opprettet TIMESTAMP
)