-- Disse forbehandlingene ble ferdigstilt uten beregning fordi mottakeren døde i etteroppgjørsåret.
-- Ny rad gir hendelsen en ny id og tidspunkt_registrert, slik at BigQuery-synken plukker opp resultatet.
INSERT INTO sak (behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                 behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                 opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                 vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet,
                 soeknad_format, sak_utland, beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak,
                 avkorting, pesysid, kilde, relatert_til, paa_vent_aarsak, sak_utland_enhet)
SELECT DISTINCT ON (s.behandling_id)
    s.behandling_id, s.sak_id, s.mottatt_tid, s.registrert_tid, s.ferdigbehandlet_tid, s.vedtak_tid,
    s.behandling_type, s.behandling_status, 'ETTEROPPGJOER_AVSLUTTET_DOEDSFALL', s.resultat_begrunnelse,
    s.behandling_metode, s.opprettet_av, s.ansvarlig_beslutter, s.aktor_id, s.dato_foerste_utbetaling,
    s.teknisk_tid, s.sak_ytelse, s.vedtak_loepende_fom, s.vedtak_loepende_tom, s.saksbehandler,
    s.ansvarlig_enhet, s.soeknad_format, s.sak_utland, s.beregning, s.sak_ytelsesgruppe,
    s.avdoede_foreldre, s.revurdering_aarsak, s.avkorting, s.pesysid, s.kilde, s.relatert_til,
    s.paa_vent_aarsak, s.sak_utland_enhet
FROM sak s
WHERE s.behandling_id IN (
    '4bf5ede1-d880-4ac2-8984-b24a24bf096f',
    '5cb975ce-2bed-4e36-9bf6-d7e05477c07d',
    'c688a9cf-195a-4c08-ac82-85a03b19eabb'
)
  AND s.behandling_type = 'ETTEROPPGJOER_FORBEHANDLING'
  AND s.behandling_status = 'FERDIGSTILT'
  AND s.behandling_resultat IS NULL
ORDER BY s.behandling_id, s.id DESC;
