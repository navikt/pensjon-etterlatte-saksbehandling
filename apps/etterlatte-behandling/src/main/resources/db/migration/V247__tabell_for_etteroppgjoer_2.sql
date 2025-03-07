create table etteroppgjoer
(
    sak_id      BIGINT
        CONSTRAINT etteroppgjoer_sak_fk_id
            REFERENCES sak (id),
    inntektsaar INT,
    opprettet   TIMESTAMP,
    status      TEXT,
    PRIMARY KEY (sak_id, inntektsaar)
)