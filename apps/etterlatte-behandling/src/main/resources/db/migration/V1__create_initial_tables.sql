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
    soekand_mottatt_dato DATE,
    behandling_opprettet TIMESTAMP,
    innsender TEXT,
    soeker TEXT,
    gjenlevende TEXT,
    avdoed TEXT,
    soesken TEXT,
    gyldighetssproving TEXT,
    status TEXT, -- gyldig_soeknad, ikke_gyldig_soeknad, avbrutt, under_behandling, fattet_vedtak, attestert, iverksatt,
    sist_endret TIMESTAMP,

);
