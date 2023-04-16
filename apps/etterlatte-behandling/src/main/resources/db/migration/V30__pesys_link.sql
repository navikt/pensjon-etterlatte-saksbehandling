CREATE TABLE pesyskopling
(
    id BIGSERIAL PRIMARY KEY,
    pesys_id VARCHAR UNIQUE NOT NULL,
    sak_id BIGINT UNIQUE NOT NULL
        CONSTRAINT pesys_sak_id_fk
            REFERENCES sak (id),
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
