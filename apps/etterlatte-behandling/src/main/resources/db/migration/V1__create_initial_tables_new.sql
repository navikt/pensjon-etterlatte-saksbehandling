CREATE TABLE sak
(
    id BIGSERIAL
        PRIMARY KEY,
    fnr VARCHAR,
    sakType VARCHAR
);


CREATE TABLE behandling
(
    id UUID
        PRIMARY KEY,
    sak_id BIGINT NOT NULL
        CONSTRAINT behandling_sak_id_fk
            REFERENCES sak (id),
    persongalleri TEXT,
    gyldighetssproving TEXT,
    status TEXT,
    fastsatt boolean
);