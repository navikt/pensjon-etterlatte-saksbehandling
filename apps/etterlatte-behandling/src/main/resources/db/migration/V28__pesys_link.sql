CREATE TABLE pesyskopling
(
    id BIGSERIAL PRIMARY KEY,
    pesys_id VARCHAR NOT NULL,
    sak_id BIGINT NOT NULL
        CONSTRAINT pesys_sak_id_fk
            REFERENCES sak (id),
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
