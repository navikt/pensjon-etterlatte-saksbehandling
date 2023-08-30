CREATE TABLE pesyskopling
(
    id BIGSERIAL PRIMARY KEY,
    pesys_id UUID UNIQUE NOT NULL
        CONSTRAINT pesys_sak_id_fk
            REFERENCES pesyssak (id),
    behandling_id UUID UNIQUE NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
