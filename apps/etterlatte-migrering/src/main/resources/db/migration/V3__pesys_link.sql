CREATE TABLE pesyskopling
(
    pesys_id UUID UNIQUE NOT NULL
        CONSTRAINT pesys_sak_id_fk
            REFERENCES pesyssak (id),
    behandling_id UUID UNIQUE NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(pesys_id, behandling_id)
);
