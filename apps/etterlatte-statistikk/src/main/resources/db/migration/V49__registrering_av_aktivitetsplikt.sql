create table aktivitetsplikt(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sak_id BIGINT NOT NULL,
    registrert TIMESTAMP NOT NULL,
    avdoed_doedsmaaned TEXT NOT NULL,
    unntak JSONB NOT NULL,
    brukers_aktivitet JSONB NOT NULL,
    aktivitetsgrad JSONB NOT NULL,
    varig_unntak BOOL NOT NULL,
    registrert_maaned TEXT NOT NULL
);

alter table aktivitetsplikt add unique (sak_id, registrert_maaned);


-- Nye kolonner for stønadsstatistikken
alter table maaned_stoenad
    add column harAktivitetsplikt TEXT,
    add column oppfyllerAktivitet TEXT,
    add column aktivitet TEXT,
    add column sanksjon TEXT;

-- Refresh av view for forrige maanedes statistikk for import
--
-- Statistikk for hva som ble utbetalt forrige måned produseres denne måneden,
-- så alle rader som er opprettet denne måneden gjelder statistikk for forrige måned
CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS
SELECT *
FROM maaned_stoenad
WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());
