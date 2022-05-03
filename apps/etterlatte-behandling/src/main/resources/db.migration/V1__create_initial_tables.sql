/*
CREATE TABLE sak
(
    id BIGSERIAL
        PRIMARY KEY,
    fnr TEXT,
    sakType TEXT
);


CREATE TABLE behandling
(
    id UUID
        PRIMARY KEY,
    sak_id BIGINT NOT NULL
        CONSTRAINT behandling_sak_id_fk
            REFERENCES sak (id),
    behandling_opprettet TIMESTAMP,
    sist_endret TIMESTAMP,
    soekand_mottatt_dato DATE,
    innsender TEXT,
    soeker TEXT,
    gjenlevende TEXT,
    avdoed TEXT,
    soesken TEXT,
    gyldighetssproving TEXT,
    status TEXT, -- gyldig_soeknad, ikke_gyldig_soeknad, avbrutt, under_behandling, fattet_vedtak, attestert, iverksatt,
);
*/