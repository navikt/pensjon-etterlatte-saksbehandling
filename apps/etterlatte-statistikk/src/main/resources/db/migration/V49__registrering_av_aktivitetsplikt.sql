create table aktivitetsplikt(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sak_id BIGINT NOT NULL,
    registrert TIMESTAMP NOT NULL,
    avdoed_doedsmaaned TEXT NOT NULL,
    unntak JSONB NOT NULL,
    brukers_aktivitet JSONB NOT NULL,
    aktivitetsgrad JSONB NOT NULL,
    varig_unntak JSONB NOT NULL,
    registrert_maaned TEXT NOT NULL
);

alter table aktivitetsplikt add unique (sak_id, registrert_maaned);
