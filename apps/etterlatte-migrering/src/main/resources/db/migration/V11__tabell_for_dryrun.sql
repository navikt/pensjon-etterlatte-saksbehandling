CREATE TABLE dryrun
(
    id UUID UNIQUE NOT NULL,
    pesys_id BIGINT UNIQUE NOT NULL
        CONSTRAINT pesys_sak_id_fk
            REFERENCES pesyssak (id),
    request JSONB,
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
);
