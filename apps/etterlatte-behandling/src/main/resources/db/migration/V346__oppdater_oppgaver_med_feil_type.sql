UPDATE oppgave
SET type = 'ETTEROPPGJOER_OPPRETT_REVURDERING'
WHERE status != 'FERDIGSTILT'
  AND type = 'VURDER_KONSEKVENS'
  AND kilde = 'HENDELSE'
  AND merknad like'Nye inntektsopplysninger fra skatt på ferdigstilt etteroppgjør 2024%'
  AND gjelder_aar = 2024
  AND opprettet <= '2026-05-05 18:15:50.089262'
  AND opprettet >= '2026-04-24 09:41:13.916277';