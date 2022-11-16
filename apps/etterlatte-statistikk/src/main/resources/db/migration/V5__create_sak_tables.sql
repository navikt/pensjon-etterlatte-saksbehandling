CREATE TABLE sak (
  id BIGSERIAL PRIMARY KEY,
  behandling_id UUID,
  sak_id BIGINT,
  mottatt_tid TIMESTAMP,
  registrert_tid TIMESTAMP,
  ferdigbehandlet_tid TIMESTAMP,
  vedtak_tid TIMESTAMP,
  behandling_type TEXT,
  behandling_status TEXT,
  behandling_resultat TEXT,
  resultat_begrunnelse TEXT,
  behandling_metode TEXT,
  opprettet_av TEXT,
  ansvarlig_beslutter TEXT,
  aktor_id TEXT,
  dato_foerste_utbetaling DATE,
  teknisk_tid TIMESTAMP,
  sak_ytelse TEXT,
  vedtak_loepende_fom DATE,
  vedtak_loepende_tom DATE,
  tidspunkt_registrert TIMESTAMP DEFAULT NOW()
);

CREATE VIEW sak_statistikk AS
    SELECT * FROM sak
    WHERE tidspunkt_registrert > NOW() - interval '2 days';