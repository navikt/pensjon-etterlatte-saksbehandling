DROP TABLE feilkjoering;
CREATE TABLE feilkjoering
(
    id UUID UNIQUE NOT NULL,
    pesys_id BIGINT NOT NULL,
    request JSONB,
    feilmelding JSONB,
    feilendeSteg TEXT,
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
);
