-- manuelt oppretter en melding for avbrutt tilbakekreving -- felter som endres fra siste melding på denne
-- behandlingen er relevante tider (settes bare til now()/default/null), og status / resultat, som settes til
-- avbrutt.
INSERT INTO sak (id, behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                 behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                 opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                 vedtak_loepende_fom, vedtak_loepende_tom, tidspunkt_registrert, saksbehandler, ansvarlig_enhet,
                 soeknad_format, sak_utland, beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak,
                 avkorting, pesysid, kilde, relatert_til, paa_vent_aarsak, sak_utland_enhet)
SELECT (DEFAULT, s.behandling_id, s.sak_id, s.mottatt_tid, NOW(), NOW(), null, s.behandling_type, 'AVBRUTT', 'AVBRUTT',
        'FEIL_KRAVGRUNNLAG', s.behandling_metode, s.opprettet_av, s.ansvarlig_beslutter, s.aktor_id,
        s.dato_foerste_utbetaling, NOW(), s.sak_ytelse, s.vedtak_loepende_fom, s.vedtak_loepende_tom, DEFAULT,
        s.saksbehandler, s.ansvarlig_enhet, s.soeknad_format, s.sak_utland, s.beregning, s.sak_ytelsesgruppe,
        s.avdoede_foreldre, s.revurdering_aarsak, s.avkorting, s.pesysid, s.kilde, s.relatert_til, s.paa_vent_aarsak,
        s.sak_utland_enhet)
FROM sak s
WHERE behandling_id = '1d7cace7-93b8-404c-8ab6-9b8ac29d476b'
  AND sak_id = 18981
  AND behandling_type = 'TILBAKEKREVING'
ORDER BY teknisk_tid DESC
LIMIT 1;